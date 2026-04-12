package plutoproject.feature.gallery.core.render.scale

import plutoproject.feature.gallery.core.render.RenderWorkspace
import plutoproject.feature.gallery.core.render.reposition.RepositionTransform

interface Scaler {
    fun scale(workspace: RenderWorkspace, transform: RepositionTransform)
}
