package plutoproject.feature.gallery.core.decode

class DecodeConstraints(
    val maxBytes: Int,
    val maxPixels: Int,
    val maxFrames: Int,
) {
    init {
        require(maxBytes > 0) { "maxBytes must be > 0" }
        require(maxPixels > 0) { "maxPixels must be > 0" }
        require(maxFrames > 0) { "maxFrames must be > 0" }
    }
}
