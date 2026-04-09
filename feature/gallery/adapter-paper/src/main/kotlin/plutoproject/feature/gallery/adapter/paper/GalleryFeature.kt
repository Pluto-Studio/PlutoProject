package plutoproject.feature.gallery.adapter.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.feature.gallery.adapter.common.*
import plutoproject.feature.gallery.adapter.paper.listeners.ChunkListener
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

@Feature(
    id = "gallery",
    platform = Platform.PAPER
)
class GalleryFeature : PaperFeature() {
    private val module = module {
        single<GalleryConfig>(createdAtStart = true) { loadConfig(saveConfig("feature/common/gallery")) }
        single<Logger> { this@GalleryFeature.logger }
        single<CoroutineScope> { coroutineScope }
        single<CoroutineContext> { coroutineScope.coroutineContext }
        singleOf(::PaperViewPort) bind ViewPort::class
        singleOf(::PaperMapUpdatePort) bind MapUpdatePort::class
        singleOf(::PaperDisplayInstanceIndex) bind DisplayInstanceIndex::class
    }

    override fun onEnable() {
        koin = featureKoin

        featureKoin {
            modules(commonModule, module)
        }

        onFeatureEnable()
        server.pluginManager.registerSuspendingEvents(ChunkListener, plugin)
    }

    override fun onDisable() {
        onFeatureDisable()
    }
}
