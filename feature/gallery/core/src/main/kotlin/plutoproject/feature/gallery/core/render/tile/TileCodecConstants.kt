package plutoproject.feature.gallery.core.render.tile

internal const val TILE_SIDE_PIXELS = 128
internal const val TILE_PIXEL_COUNT = TILE_SIDE_PIXELS * TILE_SIDE_PIXELS
internal const val MAX_SEGMENT_LENGTH = 128
internal const val MAX_PALETTE_SIZE = 256
internal const val MAX_SEGMENT_BYTES = 0xFFFF

internal fun bitsPerPixel(paletteSize: Int): Int {
    require(paletteSize in 1..MAX_PALETTE_SIZE) {
        "paletteSize must be in [1, $MAX_PALETTE_SIZE], actual=$paletteSize"
    }

    if (paletteSize == 1) {
        return 0
    }

    return 32 - Integer.numberOfLeadingZeros(paletteSize - 1)
}

internal fun decodePaletteSize(rawPaletteSize: Int): Int {
    require(rawPaletteSize in 0..0xFF) {
        "rawPaletteSize must be in [0, 255], actual=$rawPaletteSize"
    }
    return if (rawPaletteSize == 0) MAX_PALETTE_SIZE else rawPaletteSize
}

internal fun encodePaletteSize(paletteSize: Int): Int {
    require(paletteSize in 1..MAX_PALETTE_SIZE) {
        "paletteSize must be in [1, $MAX_PALETTE_SIZE], actual=$paletteSize"
    }
    return if (paletteSize == MAX_PALETTE_SIZE) 0 else paletteSize
}
