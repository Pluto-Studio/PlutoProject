package plutoproject.feature.gallery.core.render.geometry

import kotlin.math.floor
import kotlin.math.roundToInt
import plutoproject.feature.gallery.core.render.RgbaImage8888

internal object BilinearScaler : Scaler {
    override fun scale(source: RgbaImage8888, transform: DestToSourceTransform): RgbaImage8888 {
        // 先选择一个合适的 mipmap 层，避免大幅缩小时直接双线性带来的混叠。
        val selectedLevel = selectMipmapLevel(source, transform)

        // 目标坐标域是 [0, destinationWidth) x [0, destinationHeight)。
        // 对该网格中的每个像素点都进行一次反向采样，确保目标图被完整填充。
        val destinationPixels = IntArray(selectedLevel.destinationWidth * selectedLevel.destinationHeight)
        var outputIndex = 0
        var destinationY = 0
        while (destinationY < selectedLevel.destinationHeight) {
            val sourceY = selectedLevel.sourceYAt(destinationY)

            var destinationX = 0
            while (destinationX < selectedLevel.destinationWidth) {
                val sourceX = selectedLevel.sourceXAt(destinationX)
                destinationPixels[outputIndex] = bilinearSample(selectedLevel.image, sourceX, sourceY)

                destinationX++
                outputIndex++
            }

            destinationY++
        }

        return RgbaImage8888(
            width = selectedLevel.destinationWidth,
            height = selectedLevel.destinationHeight,
            pixels = destinationPixels,
        )
    }
}

private fun bilinearSample(source: RgbaImage8888, sourceX: Double, sourceY: Double): Int {
    val x0 = floor(sourceX).toInt()
    val y0 = floor(sourceY).toInt()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val tx = sourceX - x0
    val ty = sourceY - y0

    val p00 = pixelOrTransparent(source, x0, y0)
    val p10 = pixelOrTransparent(source, x1, y0)
    val p01 = pixelOrTransparent(source, x0, y1)
    val p11 = pixelOrTransparent(source, x1, y1)

    val a = bilinearChannel(p00 ushr 24, p10 ushr 24, p01 ushr 24, p11 ushr 24, tx, ty)
    val r = bilinearChannel((p00 ushr 16) and 0xFF, (p10 ushr 16) and 0xFF, (p01 ushr 16) and 0xFF, (p11 ushr 16) and 0xFF, tx, ty)
    val g = bilinearChannel((p00 ushr 8) and 0xFF, (p10 ushr 8) and 0xFF, (p01 ushr 8) and 0xFF, (p11 ushr 8) and 0xFF, tx, ty)
    val b = bilinearChannel(p00 and 0xFF, p10 and 0xFF, p01 and 0xFF, p11 and 0xFF, tx, ty)

    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun bilinearChannel(
    c00: Int,
    c10: Int,
    c01: Int,
    c11: Int,
    tx: Double,
    ty: Double,
): Int {
    val top = c00 * (1.0 - tx) + c10 * tx
    val bottom = c01 * (1.0 - tx) + c11 * tx
    return (top * (1.0 - ty) + bottom * ty).roundToInt().coerceIn(0, 255)
}

private fun pixelOrTransparent(source: RgbaImage8888, x: Int, y: Int): Int {
    // CONTAIN 场景下采样窗口可能超出源图；越界像素按透明处理。
    if (x !in 0 until source.width || y !in 0 until source.height) {
        return 0x00000000
    }
    return source.pixels[y * source.width + x]
}
