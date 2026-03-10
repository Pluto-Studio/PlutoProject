package plutoproject.feature.gallery.core.render

/**
 * 渲染阶段使用的统一像素输入格式：RGBA8888（0xAARRGGBB）。
 *
 * - 像素排列：row-major
 * - 索引公式：`pixels[y * width + x]`
 */
data class RgbaImage8888(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }

        val expectedSize = width.toLong() * height.toLong()
        require(expectedSize <= Int.MAX_VALUE.toLong()) {
            "width * height overflow: width=$width, height=$height"
        }
        require(pixels.size == expectedSize.toInt()) {
            "pixels size mismatch: expected=$expectedSize, actual=${pixels.size}"
        }
    }
}
