package plutoproject.feature.gallery.core.render.alphacomposite

import plutoproject.feature.gallery.core.render.RenderWorkspace

interface AlphaCompositor {
    fun composite(workspace: RenderWorkspace, backgroundRgb24: Int)
}
