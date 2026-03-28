package plutoproject.feature.gallery.adapter.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.gallery.adapter.common.commonModule
import plutoproject.feature.gallery.adapter.paper.commands.GalleryDebugRenderCommand
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal lateinit var featureKoin: Koin

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    private val coordinator by koin.inject<GalleryRuntimeCoordinator>()

    private val module = module {
        single<Logger>(named("gallery_logger")) { this@GalleryFeature.logger }
        singleOf(::PaperViewPort) bind ViewPort::class
        singleOf(::PaperMapUpdatePort) bind MapUpdatePort::class
        single { PaperChunkDisplayIndexStorage(plugin) }
        singleOf(::GalleryRuntimeCoordinator)
        single<CoroutineScope>(named("gallery_coroutine_scope")) { coroutineScope }
        single<CoroutineContext>(named("gallery_scheduler_context")) { coroutineScope.coroutineContext }
        single<CoroutineContext>(named("gallery_awake_context")) { coroutineScope.coroutineContext }
    }

    override fun onEnable() {
        featureKoin = koin

        koin {
            modules(commonModule, module)
        }

        AnnotationParser.parse(GalleryDebugRenderCommand)
        server.pluginManager.registerSuspendingEvents(GalleryChunkListener, plugin)
        server.pluginManager.registerEvents(GalleryPlayerListener, plugin)
    }

    override fun onDisable() {
        coordinator.onPluginDisable()
    }
}
