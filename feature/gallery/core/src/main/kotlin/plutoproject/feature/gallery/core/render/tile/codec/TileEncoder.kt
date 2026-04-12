package plutoproject.feature.gallery.core.render.tile.codec

import plutoproject.feature.gallery.core.render.tile.MAX_PALETTE_SIZE
import plutoproject.feature.gallery.core.render.tile.MAX_SEGMENT_BYTES
import plutoproject.feature.gallery.core.render.tile.MAX_SEGMENT_LENGTH
import plutoproject.feature.gallery.core.render.tile.TILE_PIXEL_COUNT

internal object TileEncoder {
    fun encode(tileMapColors: ByteArray): ByteArray {
        require(tileMapColors.size == TILE_PIXEL_COUNT) {
            "tileMapColors size must be ${TILE_PIXEL_COUNT}, actual=${tileMapColors.size}"
        }

        val paletteIndexByMapColor = IntArray(MAX_PALETTE_SIZE) { -1 }
        val paletteBytes = ByteArray(MAX_PALETTE_SIZE)
        val pixelPaletteIndexes = IntArray(TILE_PIXEL_COUNT)

        var paletteSize = 0
        for (pixelIndex in tileMapColors.indices) {
            val mapColor = tileMapColors[pixelIndex].toInt() and 0xFF
            var paletteIndex = paletteIndexByMapColor[mapColor]
            if (paletteIndex < 0) {
                paletteIndex = paletteSize
                paletteIndexByMapColor[mapColor] = paletteIndex
                paletteBytes[paletteIndex] = tileMapColors[pixelIndex]
                paletteSize++
            }
            pixelPaletteIndexes[pixelIndex] = paletteIndex
        }

        val bitsPerPixel = bitsPerPixel(paletteSize)
        val segments = encodeSegments(pixelPaletteIndexes, bitsPerPixel)
        require(segments.size <= MAX_SEGMENT_BYTES) {
            "segmentBytes overflow: ${segments.size} > ${MAX_SEGMENT_BYTES}"
        }

        val encodedPaletteSize = if (paletteSize == MAX_PALETTE_SIZE) 0 else paletteSize
        val tileData = ByteArray(1 + paletteSize + 2 + segments.size)

        var offset = 0
        tileData[offset++] = encodedPaletteSize.toByte()
        paletteBytes.copyInto(tileData, destinationOffset = offset, startIndex = 0, endIndex = paletteSize)
        offset += paletteSize

        tileData[offset++] = (segments.size and 0xFF).toByte()
        tileData[offset++] = ((segments.size ushr 8) and 0xFF).toByte()
        segments.copyInto(tileData, destinationOffset = offset)
        return tileData
    }

    private fun encodeSegments(pixelPaletteIndexes: IntArray, bitsPerPixel: Int): ByteArray {
        if (bitsPerPixel == 0) {
            return ByteArray(0)
        }

        val writer = BitWriter(initialCapacityBytes = 4096)
        var cursor = 0
        while (cursor < TILE_PIXEL_COUNT) {
            val currentIndex = pixelPaletteIndexes[cursor]
            var runLength = 1
            while (
                runLength < MAX_SEGMENT_LENGTH &&
                cursor + runLength < TILE_PIXEL_COUNT &&
                pixelPaletteIndexes[cursor + runLength] == currentIndex
            ) {
                runLength++
            }

            if (runLength >= 2) {
                writer.writeBits(0x80 or (runLength - 1), 8)
                writer.writeBits(currentIndex, bitsPerPixel)
                cursor += runLength
                continue
            }

            val literalStart = cursor
            var literalLength = 1
            cursor++

            while (literalLength < MAX_SEGMENT_LENGTH && cursor < TILE_PIXEL_COUNT) {
                val nextIndex = pixelPaletteIndexes[cursor]
                var nextRunLength = 1
                while (
                    nextRunLength < MAX_SEGMENT_LENGTH &&
                    cursor + nextRunLength < TILE_PIXEL_COUNT &&
                    pixelPaletteIndexes[cursor + nextRunLength] == nextIndex
                ) {
                    nextRunLength++
                }

                if (nextRunLength >= 2) {
                    break
                }

                literalLength++
                cursor++
            }

            writer.writeBits(literalLength - 1, 8)
            for (offset in 0 until literalLength) {
                writer.writeBits(pixelPaletteIndexes[literalStart + offset], bitsPerPixel)
            }
        }

        return writer.toByteArray()
    }

    private fun bitsPerPixel(paletteSize: Int): Int {
        require(paletteSize in 1..MAX_PALETTE_SIZE) {
            "paletteSize must be in [1, ${MAX_PALETTE_SIZE}], actual=$paletteSize"
        }

        if (paletteSize == 1) {
            return 0
        }

        return 32 - Integer.numberOfLeadingZeros(paletteSize - 1)
    }
}
