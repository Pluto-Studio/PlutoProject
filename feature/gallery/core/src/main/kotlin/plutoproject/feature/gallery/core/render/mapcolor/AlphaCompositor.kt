package plutoproject.feature.gallery.core.render.mapcolor

import plutoproject.feature.gallery.core.render.RgbaImage8888

/**
 * Alpha 处理后的中间图。
 *
 * 说明：
 * - 地图色最终只有“透明(0)”和“非透明(4..247)”两类，不存在连续 alpha。
 * - 因此这里把 alpha 语义拆成两部分：
 *   1) [transparentMask]：标记完全透明像素（alpha==0）
 *   2) [rgb24Pixels]：非透明像素与背景色合成后的 RGB24（0xRRGGBB）
 */
data class CompositedRgbImage(
    val width: Int,
    val height: Int,
    val rgb24Pixels: IntArray,
    val transparentMask: BooleanArray,
) {
    init {
        val expectedSize = width * height
        require(rgb24Pixels.size == expectedSize) {
            "rgb24Pixels size mismatch: expected=$expectedSize, actual=${rgb24Pixels.size}"
        }
        require(transparentMask.size == expectedSize) {
            "transparentMask size mismatch: expected=$expectedSize, actual=${transparentMask.size}"
        }
    }
}

fun interface AlphaCompositor {
    /**
     * 将 RGBA8888 归一化为“透明掩码 + RGB24”。
     *
     * - `alpha == 0`：标记为透明，RGB 仅作占位
     * - `alpha > 0`：与 [backgroundRgb24] 做 alpha 合成，得到不含 alpha 的 RGB24
     */
    fun composite(source: RgbaImage8888, backgroundRgb24: Int): CompositedRgbImage
}

fun defaultAlphaCompositor(): AlphaCompositor = DefaultAlphaCompositor

internal object DefaultAlphaCompositor : AlphaCompositor {
    override fun composite(source: RgbaImage8888, backgroundRgb24: Int): CompositedRgbImage {
        require(backgroundRgb24 in 0x000000..0xFFFFFF) {
            "backgroundRgb24 must be in [0x000000, 0xFFFFFF]"
        }

        val backgroundRed = (backgroundRgb24 ushr 16) and 0xFF
        val backgroundGreen = (backgroundRgb24 ushr 8) and 0xFF
        val backgroundBlue = backgroundRgb24 and 0xFF

        val rgb24Pixels = IntArray(source.pixels.size)
        val transparentMask = BooleanArray(source.pixels.size)

        var index = 0
        while (index < source.pixels.size) {
            val argb = source.pixels[index]
            val alpha = (argb ushr 24) and 0xFF

            if (alpha == 0) {
                transparentMask[index] = true
                rgb24Pixels[index] = 0
                index++
                continue
            }

            val sourceRed = (argb ushr 16) and 0xFF
            val sourceGreen = (argb ushr 8) and 0xFF
            val sourceBlue = argb and 0xFF

            val compositedRed = blendChannel(sourceRed, backgroundRed, alpha)
            val compositedGreen = blendChannel(sourceGreen, backgroundGreen, alpha)
            val compositedBlue = blendChannel(sourceBlue, backgroundBlue, alpha)
            rgb24Pixels[index] = (compositedRed shl 16) or (compositedGreen shl 8) or compositedBlue

            index++
        }

        return CompositedRgbImage(
            width = source.width,
            height = source.height,
            rgb24Pixels = rgb24Pixels,
            transparentMask = transparentMask,
        )
    }
}

private fun blendChannel(source: Int, background: Int, alpha: Int): Int {
    val inverseAlpha = 255 - alpha
    return (source * alpha + background * inverseAlpha + 127) / 255
}
