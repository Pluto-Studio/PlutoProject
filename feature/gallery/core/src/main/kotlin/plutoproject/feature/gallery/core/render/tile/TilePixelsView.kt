package plutoproject.feature.gallery.core.render.tile

class TilePixelsView internal constructor(
    private val source: ByteArray,
    private val sourceWidth: Int,
    private val tileX: Int,
    private val tileY: Int,
) {
    fun copyTo(destination: ByteArray, destinationOffset: Int = 0) {
        require(destinationOffset >= 0) { "destinationOffset must be >= 0" }
        require(destination.size - destinationOffset >= TILE_PIXEL_COUNT) {
            "destination does not have enough remaining space for one tile"
        }

        val sourceStartX = tileX * TILE_SIDE_PIXELS
        val sourceStartY = tileY * TILE_SIDE_PIXELS

        var writeOffset = destinationOffset
        var localY = 0
        while (localY < TILE_SIDE_PIXELS) {
            val sourceOffset = (sourceStartY + localY) * sourceWidth + sourceStartX
            source.copyInto(
                destination = destination,
                destinationOffset = writeOffset,
                startIndex = sourceOffset,
                endIndex = sourceOffset + TILE_SIDE_PIXELS,
            )
            writeOffset += TILE_SIDE_PIXELS
            localY++
        }
    }
}
