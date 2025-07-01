package plutoproject.feature.paper.sitV2

import org.koin.dsl.module
import plutoproject.feature.paper.api.sitV2.Sit
import plutoproject.feature.paper.sitV2.listeners.ChunkListener
import plutoproject.feature.paper.sitV2.listeners.TickListener
import plutoproject.feature.paper.sitV2.strategies.SolidBlockSitStrategy
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "sit_v2",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class SitV2Feature : PaperFeature() {
    private val featureModule = module {
        single<Sit> { SitImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        registerInternalStrategies()
        AnnotationParser.parse(SitCommand)
        server.pluginManager.registerEvents(TickListener, plugin)
        server.pluginManager.registerEvents(ChunkListener, plugin)
    }

    private fun registerInternalStrategies() {
        Sit.registerStrategy(SolidBlockSitStrategy, Int.MAX_VALUE)
    }
}
