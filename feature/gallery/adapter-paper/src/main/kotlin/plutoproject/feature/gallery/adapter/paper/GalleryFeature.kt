package plutoproject.feature.gallery.adapter.paper

import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.feature.gallery.adapter.paper.commands.GalleryDebugRenderCommand
import java.util.logging.Logger

internal lateinit var logger: Logger

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    override fun onEnable() {
        AnnotationParser.parse(GalleryDebugRenderCommand)
    }
}
