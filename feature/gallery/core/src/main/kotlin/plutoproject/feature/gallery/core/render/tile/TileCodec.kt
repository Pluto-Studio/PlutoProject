package plutoproject.feature.gallery.core.render.tile

/**
 * 将一个 128x128 的 mapColor tile 编码为 TilePool tile bytes。
 */
fun encodeTile(tileMapColors: ByteArray): ByteArray {
    require(tileMapColors.size == TILE_PIXEL_COUNT) {
        "tileMapColors size must be $TILE_PIXEL_COUNT, actual=${tileMapColors.size}"
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

    val bpp = bitsPerPixel(paletteSize)
    val segments = encodeSegments(pixelPaletteIndexes, bpp)
    require(segments.size <= MAX_SEGMENT_BYTES) {
        "segmentBytes overflow: ${segments.size} > $MAX_SEGMENT_BYTES"
    }

    val encodedPaletteSize = encodePaletteSize(paletteSize)
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

private fun encodeSegments(pixelPaletteIndexes: IntArray, bpp: Int): ByteArray {
    if (bpp == 0) {
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
            writeRunSegment(writer, runLength, currentIndex, bpp)
            cursor += runLength
            continue
        }

        val literalStart = cursor
        var literalLength = 1
        cursor += 1

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

        writeLiteralSegment(writer, pixelPaletteIndexes, literalStart, literalLength, bpp)
    }

    return writer.toByteArray()
}

private fun writeRunSegment(writer: BitWriter, runLength: Int, index: Int, bpp: Int) {
    val control = 0x80 or (runLength - 1)
    writer.writeBits(control, 8)
    writer.writeBits(index, bpp)
}

private fun writeLiteralSegment(
    writer: BitWriter,
    indexes: IntArray,
    start: Int,
    length: Int,
    bpp: Int,
) {
    val control = length - 1
    writer.writeBits(control, 8)
    for (i in 0 until length) {
        writer.writeBits(indexes[start + i], bpp)
    }
}

/**
 * 将 TilePool tile bytes 解码为 128x128 的 mapColor tile。
 */
fun decodeTile(tileData: ByteArray): ByteArray {
    require(tileData.isNotEmpty()) { "tileData must not be empty" }

    var offset = 0
    val rawPaletteSize = tileData[offset++].toInt() and 0xFF
    val paletteSize = decodePaletteSize(rawPaletteSize)
    require(tileData.size >= 1 + paletteSize + 2) {
        "tileData is too short for palette and segmentBytes: size=${tileData.size}, paletteSize=$paletteSize"
    }

    val palette = ByteArray(paletteSize)
    tileData.copyInto(palette, destinationOffset = 0, startIndex = offset, endIndex = offset + paletteSize)
    offset += paletteSize

    val segmentBytes =
        (tileData[offset].toInt() and 0xFF) or ((tileData[offset + 1].toInt() and 0xFF) shl 8)
    offset += 2

    require(tileData.size == offset + segmentBytes) {
        "tileData size mismatch: expected=${offset + segmentBytes}, actual=${tileData.size}"
    }

    val bpp = bitsPerPixel(paletteSize)
    val pixels = ByteArray(TILE_PIXEL_COUNT)

    if (bpp == 0 && segmentBytes == 0) {
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
            val paletteIndex = reader.readBits(bpp)
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
            val paletteIndex = reader.readBits(bpp)
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
