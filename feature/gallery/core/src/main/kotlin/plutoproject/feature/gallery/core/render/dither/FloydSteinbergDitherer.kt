package plutoproject.feature.gallery.core.render.dither

import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.util.clampToByte
import plutoproject.feature.gallery.core.render.quantize.MapColorPalette
import plutoproject.feature.gallery.core.render.quantize.Quantizer

object FloydSteinbergDitherer : Ditherer {
    override fun dither(workspace: RenderWorkspace, quantizer: Quantizer) {
        val source = workspace.pixelBuffer ?: error("pixelBuffer must not be null before dithering")
        val transparentMask = workspace.transparentMask
        val width = source.width
        val height = source.height
        val result = ByteArray(source.pixels.size)

        var currentRowErrorRed = IntArray(width + 2)
        var currentRowErrorGreen = IntArray(width + 2)
        var currentRowErrorBlue = IntArray(width + 2)
        var nextRowErrorRed = IntArray(width + 2)
        var nextRowErrorGreen = IntArray(width + 2)
        var nextRowErrorBlue = IntArray(width + 2)

        var index = 0
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (transparentMask?.isTransparent(index) == true) {
                    result[index] = 0
                    x++
                    index++
                    continue
                }

                val rgb24 = source.pixels[index] and 0x00FFFFFF
                val red = clampToByte(((rgb24 ushr 16) and 0xFF) + roundFixed16(currentRowErrorRed[x + 1]))
                val green = clampToByte(((rgb24 ushr 8) and 0xFF) + roundFixed16(currentRowErrorGreen[x + 1]))
                val blue = clampToByte((rgb24 and 0xFF) + roundFixed16(currentRowErrorBlue[x + 1]))
                val adjustedRgb24 = (red shl 16) or (green shl 8) or blue

                val mapColor = quantizer.quantize(adjustedRgb24)
                result[index] = mapColor

                val mappedRgb24 = MapColorPalette.rgb24OfMapColor(mapColor)
                val redError = red - ((mappedRgb24 ushr 16) and 0xFF)
                val greenError = green - ((mappedRgb24 ushr 8) and 0xFF)
                val blueError = blue - (mappedRgb24 and 0xFF)

                diffuseError(currentRowErrorRed, nextRowErrorRed, x, redError)
                diffuseError(currentRowErrorGreen, nextRowErrorGreen, x, greenError)
                diffuseError(currentRowErrorBlue, nextRowErrorBlue, x, blueError)

                x++
                index++
            }

            val swapRed = currentRowErrorRed
            currentRowErrorRed = nextRowErrorRed
            nextRowErrorRed = swapRed
            nextRowErrorRed.fill(0)

            val swapGreen = currentRowErrorGreen
            currentRowErrorGreen = nextRowErrorGreen
            nextRowErrorGreen = swapGreen
            nextRowErrorGreen.fill(0)

            val swapBlue = currentRowErrorBlue
            currentRowErrorBlue = nextRowErrorBlue
            nextRowErrorBlue = swapBlue
            nextRowErrorBlue.fill(0)

            y++
        }

        workspace.writeMapColorPixels(result)
    }
}

private fun roundFixed16(fixed16Value: Int): Int {
    return if (fixed16Value >= 0) (fixed16Value + 8) shr 4 else (fixed16Value - 8) shr 4
}

private fun diffuseError(currentRowError: IntArray, nextRowError: IntArray, x: Int, error: Int) {
    currentRowError[x + 2] += error * 7
    nextRowError[x] += error * 3
    nextRowError[x + 1] += error * 5
    nextRowError[x + 2] += error
}
