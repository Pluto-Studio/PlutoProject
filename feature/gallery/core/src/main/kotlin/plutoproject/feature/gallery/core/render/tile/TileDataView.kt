package plutoproject.feature.gallery.core.render.tile

class TileDataView internal constructor(
    private val blob: ByteArray,
    private val start: Int,
    private val end: Int,
) {
    val size: Int = end - start

    fun copyTo(destination: ByteArray, destinationOffset: Int = 0) {
        require(destinationOffset >= 0) { "destinationOffset must be >= 0" }
        require(destination.size - destinationOffset >= size) {
            "destination does not have enough remaining space for tile data"
        }
        blob.copyInto(
            destination = destination,
            destinationOffset = destinationOffset,
            startIndex = start,
            endIndex = end,
        )
    }

    fun toByteArray(): ByteArray = ByteArray(size).also { copyTo(it) }
}
