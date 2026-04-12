package plutoproject.feature.gallery.core.render.dither

import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.render.quantize.Quantizer

interface Ditherer {
    fun dither(workspace: RenderWorkspace, quantizer: Quantizer)
}
