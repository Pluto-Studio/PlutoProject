package plutoproject.feature.gallery.core.render.tile.codec

import plutoproject.feature.gallery.core.render.tile.MAX_PALETTE_SIZE
import plutoproject.feature.gallery.core.render.tile.TILE_PIXEL_COUNT

internal object TileDecoder {
    fun decode(tileData: ByteArray): ByteArray {
        require(tileData.isNotEmpty()) { "tileData must not be empty" }

        var offset = 0
        val rawPaletteSize = tileData[offset++].toInt() and 0xFF
        val paletteSize = decodePaletteSize(rawPaletteSize)
        require(tileData.size >= 1 + paletteSize + 2) {
            "tileData is too short for palette and segmentBytes: size=${tileData.size}, paletteSize=$paletteSize"
        }

        val palette = ByteArray(paletteSize)
        tileData.copyInto(
            destination = palette,
            destinationOffset = 0,
            startIndex = offset,
            endIndex = offset + paletteSize,
        )
        offset += paletteSize

        val segmentBytes =
            (tileData[offset].toInt() and 0xFF) or ((tileData[offset + 1].toInt() and 0xFF) shl 8)
        offset += 2

        require(tileData.size == offset + segmentBytes) {
            "tileData size mismatch: expected=${offset + segmentBytes}, actual=${tileData.size}"
        }

        val bitsPerPixel = bitsPerPixel(paletteSize)
        val pixels = ByteArray(TILE_PIXEL_COUNT)

        if (bitsPerPixel == 0 && segmentBytes == 0) {
            pixels.fill(palette[0])
            return pixels
        }

        val reader = BitReader(tileData, startOffset = offset, length = segmentBytes)
        var pixelOffset = 0

        while (pixelOffset < TILE_PIXEL_COUNT) {
            require(reader.remainingBits() >= 8) {
                "unexpected end of segment stream before filling tile pixels"
            }

            val control = reader.readBits(8)
            val isRun = (control and 0x80) != 0
            val segmentLength = (control and 0x7F) + 1

            require(pixelOffset + segmentLength <= TILE_PIXEL_COUNT) {
                "segment length overflow: pixelOffset=$pixelOffset, segmentLength=$segmentLength"
            }

            if (isRun) {
                val paletteIndex = reader.readBits(bitsPerPixel)
                require(paletteIndex < paletteSize) {
                    "palette index out of range in run segment: index=$paletteIndex, paletteSize=$paletteSize"
                }

                val value = palette[paletteIndex]
                repeat(segmentLength) {
                    pixels[pixelOffset++] = value
                }
                continue
            }

            repeat(segmentLength) {
                val paletteIndex = reader.readBits(bitsPerPixel)
                require(paletteIndex < paletteSize) {
                    "palette index out of range in literal segment: index=$paletteIndex, paletteSize=$paletteSize"
                }
                pixels[pixelOffset++] = palette[paletteIndex]
            }
        }

        val trailingBits = reader.remainingBits()
        require(trailingBits <= 7) {
            "unexpected trailing segment data: trailingBits=$trailingBits"
        }
        repeat(trailingBits) {
            require(reader.readBits(1) == 0) { "non-zero padding bit found at segment tail" }
        }

        return pixels
    }

    private fun decodePaletteSize(rawPaletteSize: Int): Int {
        require(rawPaletteSize in 0..0xFF) {
            "rawPaletteSize must be in [0, 255], actual=$rawPaletteSize"
        }
        return if (rawPaletteSize == 0) MAX_PALETTE_SIZE else rawPaletteSize
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
