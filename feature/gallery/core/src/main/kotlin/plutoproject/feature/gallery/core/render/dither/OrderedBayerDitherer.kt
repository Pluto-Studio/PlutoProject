package plutoproject.feature.gallery.core.render.dither

import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.util.clampToByte
import plutoproject.feature.gallery.core.render.quantize.Quantizer

private const val ORDERED_DITHER_STRENGTH = 16

private val ORDERED_BAYER_4X4 = intArrayOf(
    0, 8, 2, 10,
    12, 4, 14, 6,
    3, 11, 1, 9,
    15, 7, 13, 5,
)

object OrderedBayerDitherer : Ditherer {
    override fun dither(workspace: RenderWorkspace, quantizer: Quantizer) {
        val source = workspace.pixelBuffer ?: error("pixelBuffer must not be null before dithering")
        val transparentMask = workspace.transparentMask
        val result = ByteArray(source.pixels.size)

        var index = 0
        var y = 0
        while (y < source.height) {
            var x = 0
            while (x < source.width) {
                if (transparentMask?.isTransparent(index) == true) {
                    result[index] = 0
                    x++
                    index++
                    continue
                }

                val rgb24 = source.pixels[index] and 0x00FFFFFF
                val ditherOffset = orderedOffsetAt(x, y)

                val red = ((rgb24 ushr 16) and 0xFF) + ditherOffset
                val green = ((rgb24 ushr 8) and 0xFF) + ditherOffset
                val blue = (rgb24 and 0xFF) + ditherOffset

                val adjustedRgb24 =
                    (clampToByte(red) shl 16) or (clampToByte(green) shl 8) or clampToByte(blue)
                result[index] = quantizer.quantize(adjustedRgb24)

                x++
                index++
            }
            y++
        }

        workspace.writeMapColorPixels(result)
    }
}

private fun orderedOffsetAt(x: Int, y: Int): Int {
    val matrixValue = ORDERED_BAYER_4X4[(y and 3) * 4 + (x and 3)]
    return (matrixValue - 8) * ORDERED_DITHER_STRENGTH / 16
}
