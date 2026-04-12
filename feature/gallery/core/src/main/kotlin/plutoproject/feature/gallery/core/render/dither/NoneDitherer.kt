package plutoproject.feature.gallery.core.render.dither

import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.render.quantize.Quantizer

object NoneDitherer : Ditherer {
    override fun dither(workspace: RenderWorkspace, quantizer: Quantizer) {
        val source = workspace.pixelBuffer ?: error("pixelBuffer must not be null before dithering")
        val transparentMask = workspace.transparentMask
        val result = ByteArray(source.pixels.size)

        var index = 0
        while (index < source.pixels.size) {
            if (transparentMask?.isTransparent(index) == true) {
                result[index] = 0
                index++
                continue
            }

            result[index] = quantizer.quantize(source.pixels[index] and 0x00FFFFFF)
            index++
        }

        workspace.writeMapColorPixels(result)
    }
}
