package plutoproject.feature.gallery.core.render.tile

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
