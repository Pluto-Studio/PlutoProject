package plutoproject.feature.gallery.adapter.paper

import org.koin.core.qualifier.named
import org.koin.dsl.module
import plutoproject.feature.gallery.adapter.common.commonModule
import plutoproject.feature.gallery.adapter.paper.commands.GalleryDebugRenderCommand
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import java.util.logging.Logger

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    private val module = module {
        single<Logger>(named("gallery_logger")) { this@GalleryFeature.logger }
    }

    override fun onEnable() {
        configureKoin {
            modules(commonModule, module)
        }

        AnnotationParser.parse(GalleryDebugRenderCommand)
    }
}
