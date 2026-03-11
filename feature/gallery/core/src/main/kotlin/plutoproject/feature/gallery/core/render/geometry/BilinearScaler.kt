package plutoproject.feature.gallery.core.render.geometry

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import plutoproject.feature.gallery.core.render.RgbaImage8888

internal object BilinearScaler : Scaler {
    override fun scale(source: RgbaImage8888, transform: DestToSourceTransform): RgbaImage8888 {
        // 先选择一个合适的 mipmap 层，避免大幅缩小时直接双线性带来的混叠。
        val selectedLevel = selectMipmapLevel(source, transform)

        // 目标坐标域是 [0, destinationWidth) x [0, destinationHeight)。
        // 对该网格中的每个像素点都进行一次反向采样，确保目标图被完整填充。
        val destinationPixels = IntArray(transform.destinationWidth * transform.destinationHeight)
        var outputIndex = 0
        var destinationY = 0
        while (destinationY < transform.destinationHeight) {
            val sourceY = selectedLevel.sourceYAt(destinationY)

            var destinationX = 0
            while (destinationX < transform.destinationWidth) {
                val sourceX = selectedLevel.sourceXAt(destinationX)
                destinationPixels[outputIndex] = bilinearSample(selectedLevel.image, sourceX, sourceY)

                destinationX++
                outputIndex++
            }

            destinationY++
        }

        return RgbaImage8888(
            width = transform.destinationWidth,
            height = transform.destinationHeight,
            pixels = destinationPixels,
        )
    }
}

private data class MipmapSelection(
    val image: RgbaImage8888,
    val destinationWidth: Int,
    val destinationHeight: Int,
    val sourceStartX: Double,
    val sourceStartY: Double,
    val sourceSpanWidth: Double,
    val sourceSpanHeight: Double,
) {
    fun sourceXAt(destinationX: Int): Double {
        return sourceStartX + ((destinationX + 0.5) / imageWidthScale) * sourceSpanWidth - 0.5
    }

    fun sourceYAt(destinationY: Int): Double {
        return sourceStartY + ((destinationY + 0.5) / imageHeightScale) * sourceSpanHeight - 0.5
    }

    private val imageWidthScale: Double
        get() = destinationWidth.toDouble()

    private val imageHeightScale: Double
        get() = destinationHeight.toDouble()
}

private fun selectMipmapLevel(
    source: RgbaImage8888,
    transform: DestToSourceTransform,
): MipmapSelection {
    var currentImage = source
    var currentStartX = transform.sourceStartX
    var currentStartY = transform.sourceStartY
    var currentSpanWidth = transform.sourceSpanWidth
    var currentSpanHeight = transform.sourceSpanHeight

    while (
        (currentSpanWidth > transform.destinationWidth * 2.0 ||
            currentSpanHeight > transform.destinationHeight * 2.0) &&
        (currentImage.width > 1 || currentImage.height > 1)
    ) {
        // 每次逐半下采样，同时把采样窗口映射到新层级坐标系。
        val nextImage = halfDownsample(currentImage)
        if (nextImage.width == currentImage.width && nextImage.height == currentImage.height) {
            break
        }

        currentImage = nextImage
        currentStartX /= 2.0
        currentStartY /= 2.0
        currentSpanWidth /= 2.0
        currentSpanHeight /= 2.0
    }

    return MipmapSelection(
        image = currentImage,
        destinationWidth = transform.destinationWidth,
        destinationHeight = transform.destinationHeight,
        sourceStartX = currentStartX,
        sourceStartY = currentStartY,
        sourceSpanWidth = currentSpanWidth,
        sourceSpanHeight = currentSpanHeight,
    )
}

private fun halfDownsample(source: RgbaImage8888): RgbaImage8888 {
    val destinationWidth = max(1, (source.width + 1) / 2)
    val destinationHeight = max(1, (source.height + 1) / 2)
    val destinationPixels = IntArray(destinationWidth * destinationHeight)

    var destinationY = 0
    var destinationIndex = 0
    while (destinationY < destinationHeight) {
        val sourceY0 = destinationY * 2
        val sourceY1 = min(sourceY0 + 1, source.height - 1)

        var destinationX = 0
        while (destinationX < destinationWidth) {
            val sourceX0 = destinationX * 2
            val sourceX1 = min(sourceX0 + 1, source.width - 1)

            val p00 = source.pixels[sourceY0 * source.width + sourceX0]
            val p10 = source.pixels[sourceY0 * source.width + sourceX1]
            val p01 = source.pixels[sourceY1 * source.width + sourceX0]
            val p11 = source.pixels[sourceY1 * source.width + sourceX1]

            destinationPixels[destinationIndex] = average4(p00, p10, p01, p11)

            destinationX++
            destinationIndex++
        }

        destinationY++
    }

    return RgbaImage8888(
        width = destinationWidth,
        height = destinationHeight,
        pixels = destinationPixels,
    )
}

private fun average4(p00: Int, p10: Int, p01: Int, p11: Int): Int {
    val a = ((p00 ushr 24) and 0xFF) + ((p10 ushr 24) and 0xFF) + ((p01 ushr 24) and 0xFF) + ((p11 ushr 24) and 0xFF)
    val r = ((p00 ushr 16) and 0xFF) + ((p10 ushr 16) and 0xFF) + ((p01 ushr 16) and 0xFF) + ((p11 ushr 16) and 0xFF)
    val g = ((p00 ushr 8) and 0xFF) + ((p10 ushr 8) and 0xFF) + ((p01 ushr 8) and 0xFF) + ((p11 ushr 8) and 0xFF)
    val b = (p00 and 0xFF) + (p10 and 0xFF) + (p01 and 0xFF) + (p11 and 0xFF)

    return ((a / 4) shl 24) or ((r / 4) shl 16) or ((g / 4) shl 8) or (b / 4)
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
