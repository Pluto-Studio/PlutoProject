package plutoproject.feature.gallery.adapter.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.CoroutineScope
import org.koin.core.Koin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.gallery.adapter.common.GalleryConfig
import plutoproject.feature.gallery.adapter.common.commonModule
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal lateinit var koin: Koin

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    private val coordinator by featureKoin.inject<GalleryRuntimeCoordinator>()

    private val module = module {
        single<GalleryConfig>(createdAtStart = true) { loadConfig(saveConfig("feature/common/gallery")) }
        single<Logger> { this@GalleryFeature.logger }
        single<PaperChunkDisplayIndexStorage> { PaperChunkDisplayIndexStorage(plugin) }
        single<CoroutineScope>() { coroutineScope }
        single<CoroutineContext>() { coroutineScope.coroutineContext }
        singleOf(::PaperViewPort) bind ViewPort::class
        singleOf(::PaperMapUpdatePort) bind MapUpdatePort::class
        singleOf(::GalleryRuntimeCoordinator)
    }

    override fun onEnable() {
        koin = featureKoin

        featureKoin {
            modules(commonModule, module)
        }

        server.pluginManager.registerSuspendingEvents(GalleryChunkListener, plugin)
        server.pluginManager.registerEvents(GalleryPlayerListener, plugin)
    }

    override fun onDisable() {
        coordinator.onPluginDisable()
    }
}
