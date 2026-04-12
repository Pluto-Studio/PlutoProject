package plutoproject.feature.gallery.core.render.scale

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import plutoproject.feature.gallery.core.render.PixelBuffer
import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.render.reposition.RepositionTransform

object BilinearScaler : Scaler {
    override fun scale(workspace: RenderWorkspace, transform: RepositionTransform) {
        val source = workspace.pixelBuffer ?: error("pixelBuffer must not be null before scaling")
        val selectedLevel = selectMipmapLevel(source, transform)
        val destinationPixels = IntArray(selectedLevel.destinationWidth * selectedLevel.destinationHeight)

        var outputIndex = 0
        var destinationY = 0
        while (destinationY < selectedLevel.destinationHeight) {
            val sourceY = selectedLevel.sourceYAt(destinationY)

            var destinationX = 0
            while (destinationX < selectedLevel.destinationWidth) {
                val sourceX = selectedLevel.sourceXAt(destinationX)
                destinationPixels[outputIndex] = bilinearSample(selectedLevel.pixelBuffer, sourceX, sourceY)

                destinationX++
                outputIndex++
            }

            destinationY++
        }

        workspace.pixelBuffer = PixelBuffer(
            width = selectedLevel.destinationWidth,
            height = selectedLevel.destinationHeight,
            pixels = destinationPixels,
        )
    }
}

private data class MipmapSelection(
    val pixelBuffer: PixelBuffer,
    val destinationWidth: Int,
    val destinationHeight: Int,
    val sourceStartX: Double,
    val sourceStartY: Double,
    val sourceSpanWidth: Double,
    val sourceSpanHeight: Double,
) {
    fun sourceXAt(destinationX: Int): Double {
        return sourceStartX + ((destinationX + 0.5) / destinationWidth) * sourceSpanWidth
    }

    fun sourceYAt(destinationY: Int): Double {
        return sourceStartY + ((destinationY + 0.5) / destinationHeight) * sourceSpanHeight
    }
}

private fun selectMipmapLevel(
    source: PixelBuffer,
    transform: RepositionTransform,
): MipmapSelection {
    var currentPixelBuffer = source
    var currentStartX = transform.sourceStartX
    var currentStartY = transform.sourceStartY
    var currentSpanWidth = transform.sourceSpanWidth
    var currentSpanHeight = transform.sourceSpanHeight

    while (
        (currentSpanWidth > transform.destinationWidth * 2.0 ||
            currentSpanHeight > transform.destinationHeight * 2.0) &&
        (currentPixelBuffer.width > 1 || currentPixelBuffer.height > 1)
    ) {
        val nextPixelBuffer = halfDownsample(currentPixelBuffer)
        if (nextPixelBuffer.width == currentPixelBuffer.width && nextPixelBuffer.height == currentPixelBuffer.height) {
            break
        }

        currentPixelBuffer = nextPixelBuffer
        currentStartX /= 2.0
        currentStartY /= 2.0
        currentSpanWidth /= 2.0
        currentSpanHeight /= 2.0
    }

    return MipmapSelection(
        pixelBuffer = currentPixelBuffer,
        destinationWidth = transform.destinationWidth,
        destinationHeight = transform.destinationHeight,
        sourceStartX = currentStartX,
        sourceStartY = currentStartY,
        sourceSpanWidth = currentSpanWidth,
        sourceSpanHeight = currentSpanHeight,
    )
}

private fun halfDownsample(source: PixelBuffer): PixelBuffer {
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

    return PixelBuffer(
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

private fun bilinearSample(source: PixelBuffer, sourceX: Double, sourceY: Double): Int {
    val x0 = floor(sourceX - 0.5).toInt()
    val y0 = floor(sourceY - 0.5).toInt()
    val x1 = x0 + 1
    val y1 = y0 + 1

    val tx = sourceX - (x0 + 0.5)
    val ty = sourceY - (y0 + 0.5)

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

private fun pixelOrTransparent(source: PixelBuffer, x: Int, y: Int): Int {
    if (x !in 0 until source.width || y !in 0 until source.height) {
        return 0x00000000
    }
    return source.pixels[y * source.width + x]
}
