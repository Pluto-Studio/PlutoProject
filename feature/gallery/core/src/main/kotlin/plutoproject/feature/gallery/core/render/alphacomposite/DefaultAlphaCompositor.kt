package plutoproject.feature.gallery.core.render.alphacomposite

import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.render.TransparentMask

object DefaultAlphaCompositor : AlphaCompositor {
    override fun composite(workspace: RenderWorkspace, backgroundRgb24: Int) {
        require(backgroundRgb24 in 0x000000..0xFFFFFF) {
            "backgroundRgb24 must be in [0x000000, 0xFFFFFF]"
        }

        val pixelBuffer = workspace.pixelBuffer ?: error("pixelBuffer must not be null before alpha compositing")
        val pixels = pixelBuffer.pixels

        val backgroundRed = (backgroundRgb24 ushr 16) and 0xFF
        val backgroundGreen = (backgroundRgb24 ushr 8) and 0xFF
        val backgroundBlue = backgroundRgb24 and 0xFF

        var transparentMask: TransparentMask? = null

        var index = 0
        while (index < pixels.size) {
            val argb = pixels[index]
            val alpha = (argb ushr 24) and 0xFF

            if (alpha == 0) {
                if (transparentMask == null) {
                    transparentMask = TransparentMask(pixelBuffer.width, pixelBuffer.height)
                }
                transparentMask.setTransparent(index)
                pixels[index] = 0
                index++
                continue
            }

            val sourceRed = (argb ushr 16) and 0xFF
            val sourceGreen = (argb ushr 8) and 0xFF
            val sourceBlue = argb and 0xFF

            if (alpha == 0xFF) {
                pixels[index] = (sourceRed shl 16) or (sourceGreen shl 8) or sourceBlue
                index++
                continue
            }

            val compositedRed = blendChannel(sourceRed, backgroundRed, alpha)
            val compositedGreen = blendChannel(sourceGreen, backgroundGreen, alpha)
            val compositedBlue = blendChannel(sourceBlue, backgroundBlue, alpha)
            pixels[index] = (compositedRed shl 16) or (compositedGreen shl 8) or compositedBlue

            index++
        }

        workspace.transparentMask = transparentMask
    }
}

private fun blendChannel(source: Int, background: Int, alpha: Int): Int {
    val inverseAlpha = 255 - alpha
    return (source * alpha + background * inverseAlpha + 127) / 255
}
