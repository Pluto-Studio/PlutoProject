package plutoproject.feature.gallery.core.render.geometry

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.roundToInt
import plutoproject.feature.gallery.core.render.RgbaImage8888

internal object LanczosScaler : Scaler {
    override fun scale(source: RgbaImage8888, transform: DestToSourceTransform): RgbaImage8888 {
        val selectedLevel = selectMipmapLevel(source, transform)

        val sourceScaleX = selectedLevel.sourceSpanWidth / selectedLevel.destinationWidth.toDouble()
        val sourceScaleY = selectedLevel.sourceSpanHeight / selectedLevel.destinationHeight.toDouble()

        val xTapsByDestinationX = Array(selectedLevel.destinationWidth) { destinationX ->
            val sourceX = selectedLevel.sourceXAt(destinationX)
            buildLanczosTaps(sourceX, sourceScaleX)
        }
        val yTapsByDestinationY = Array(selectedLevel.destinationHeight) { destinationY ->
            val sourceY = selectedLevel.sourceYAt(destinationY)
            buildLanczosTaps(sourceY, sourceScaleY)
        }

        val destinationPixels = IntArray(selectedLevel.destinationWidth * selectedLevel.destinationHeight)
        var destinationIndex = 0
        var destinationY = 0
        while (destinationY < selectedLevel.destinationHeight) {
            val yTaps = yTapsByDestinationY[destinationY]

            var destinationX = 0
            while (destinationX < selectedLevel.destinationWidth) {
                val xTaps = xTapsByDestinationX[destinationX]
                destinationPixels[destinationIndex] = lanczosSample(selectedLevel.image, xTaps, yTaps)

                destinationX++
                destinationIndex++
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

private const val LOBES = 3.0
private const val EPSILON = 1e-12

private data class LanczosTap(
    val sourceIndex: Int,
    val weight: Double,
)

private fun buildLanczosTaps(sourceCenter: Double, sourceScale: Double): Array<LanczosTap> {
    val scale = max(1.0, sourceScale)
    val support = LOBES * scale
    val left = ceil(sourceCenter - support).toInt()
    val right = floor(sourceCenter + support).toInt()

    if (left > right) {
        return emptyArray()
    }

    val taps = ArrayList<LanczosTap>(right - left + 1)
    var sourceX = left
    while (sourceX <= right) {
        val distance = sourceCenter - sourceX
        val normalizedDistance = distance / scale
        val weight = lanczosKernel(normalizedDistance)
        if (abs(weight) > EPSILON) {
            taps.add(LanczosTap(sourceIndex = sourceX, weight = weight))
        }
        sourceX++
    }

    return taps.toTypedArray()
}

private fun lanczosKernel(x: Double): Double {
    val absoluteX = abs(x)
    if (absoluteX >= LOBES) {
        return 0.0
    }
    if (absoluteX < EPSILON) {
        return 1.0
    }
    return sinc(x) * sinc(x / LOBES)
}

private fun sinc(x: Double): Double {
    if (abs(x) < EPSILON) {
        return 1.0
    }
    val pix = PI * x
    return sin(pix) / pix
}

private fun lanczosSample(source: RgbaImage8888, xTaps: Array<LanczosTap>, yTaps: Array<LanczosTap>): Int {
    if (xTaps.isEmpty() || yTaps.isEmpty()) {
        return 0x00000000
    }

    var weightSum = 0.0
    var alphaSum = 0.0
    var redSum = 0.0
    var greenSum = 0.0
    var blueSum = 0.0

    for (yTap in yTaps) {
        val sourceY = yTap.sourceIndex
        for (xTap in xTaps) {
            val sourceX = xTap.sourceIndex
            val weight = xTap.weight * yTap.weight

            val pixel = lanczosPixelOrTransparent(source, sourceX, sourceY)
            val alpha = ((pixel ushr 24) and 0xFF) / 255.0
            val red = (pixel ushr 16) and 0xFF
            val green = (pixel ushr 8) and 0xFF
            val blue = pixel and 0xFF

            weightSum += weight
            alphaSum += alpha * weight
            redSum += red * alpha * weight
            greenSum += green * alpha * weight
            blueSum += blue * alpha * weight
        }
    }

    if (abs(weightSum) < EPSILON) {
        return 0x00000000
    }

    val normalizedAlpha = (alphaSum / weightSum).coerceIn(0.0, 1.0)
    if (normalizedAlpha <= EPSILON) {
        return 0x00000000
    }

    val normalizedRed = ((redSum / weightSum) / normalizedAlpha).coerceIn(0.0, 255.0)
    val normalizedGreen = ((greenSum / weightSum) / normalizedAlpha).coerceIn(0.0, 255.0)
    val normalizedBlue = ((blueSum / weightSum) / normalizedAlpha).coerceIn(0.0, 255.0)

    val alphaChannel = (normalizedAlpha * 255.0).roundToInt().coerceIn(0, 255)
    val redChannel = normalizedRed.roundToInt().coerceIn(0, 255)
    val greenChannel = normalizedGreen.roundToInt().coerceIn(0, 255)
    val blueChannel = normalizedBlue.roundToInt().coerceIn(0, 255)

    return (alphaChannel shl 24) or (redChannel shl 16) or (greenChannel shl 8) or blueChannel
}

private fun lanczosPixelOrTransparent(source: RgbaImage8888, x: Int, y: Int): Int {
    if (x !in 0 until source.width || y !in 0 until source.height) {
        return 0x00000000
    }
    return source.pixels[y * source.width + x]
}
