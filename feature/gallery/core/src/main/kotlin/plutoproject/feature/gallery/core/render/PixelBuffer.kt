package plutoproject.feature.gallery.core.render

/**
 * 一块 32-bit RGBA 或 24-bit RGB 像素区域。
 */
class PixelBuffer(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require((width.toLong() * height.toLong()) <= Int.MAX_VALUE) { "Pixel count overflow (> ${Int.MAX_VALUE})" }
        require(pixels.size == width * height) { "Pixels size must be equal to width * height" }
    }
}
