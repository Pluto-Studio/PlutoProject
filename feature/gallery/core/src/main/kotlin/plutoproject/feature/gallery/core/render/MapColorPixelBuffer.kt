package plutoproject.feature.gallery.core.render

/**
 * 一块地图画颜色像素区域。
 */
class MapColorPixelBuffer(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require((width.toLong() * height.toLong()) <= Int.MAX_VALUE) { "Pixel count overflow (> ${Int.MAX_VALUE})" }
        require(pixels.size == width * height) { "Pixels size must be equal to width * height" }
    }
}
