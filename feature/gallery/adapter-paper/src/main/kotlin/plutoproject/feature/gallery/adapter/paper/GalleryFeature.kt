package plutoproject.feature.gallery.adapter.paper

import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.gallery.adapter.common.commonModule
import plutoproject.feature.gallery.adapter.paper.commands.GalleryDebugRenderCommand
import plutoproject.feature.gallery.core.MapUpdatePort
import plutoproject.feature.gallery.core.ViewPort
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    private val module = module {
        single<Logger>(named("gallery_logger")) { this@GalleryFeature.logger }
        singleOf(::PaperViewPort) bind ViewPort::class
        singleOf(::PaperMapUpdatePort) bind MapUpdatePort::class
        single<CoroutineScope>(named("gallery_coroutine_scope")) { PluginScope }
        single<CoroutineContext>(named("gallery_scheduler_context")) { PluginScope.coroutineContext }
        single<CoroutineContext>(named("gallery_awake_context")) { PluginScope.coroutineContext }
    }

    override fun onEnable() {
        configureKoin {
            modules(commonModule, module)
        }

        AnnotationParser.parse(GalleryDebugRenderCommand)
    }
}
