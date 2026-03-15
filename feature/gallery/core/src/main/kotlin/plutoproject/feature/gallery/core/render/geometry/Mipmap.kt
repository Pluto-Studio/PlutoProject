package plutoproject.feature.gallery.core.render.geometry

import kotlin.math.max
import kotlin.math.min
import plutoproject.feature.gallery.core.render.RgbaImage8888

internal data class MipmapSelection(
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

internal fun selectMipmapLevel(
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
