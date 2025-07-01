package plutoproject.feature.paper.sit

import org.koin.dsl.module
import plutoproject.feature.paper.api.sit.Sit
import plutoproject.feature.paper.sit.listeners.BlockListener
import plutoproject.feature.paper.sit.listeners.ChunkListener
import plutoproject.feature.paper.sit.listeners.PlayerListener
import plutoproject.feature.paper.sit.listeners.ServerListener
import plutoproject.feature.paper.sit.strategies.*
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "sit",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class SitFeature : PaperFeature() {
    private val featureModule = module {
        single<Sit> { SitImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        registerInternalStrategies()
        AnnotationParser.parse(SitCommand)
        server.pluginManager.registerEvents(ServerListener, plugin)
        server.pluginManager.registerEvents(ChunkListener, plugin)
        server.pluginManager.registerEvents(PlayerListener, plugin)
        server.pluginManager.registerEvents(BlockListener, plugin)
    }

    private fun registerInternalStrategies() {
        Sit.registerStrategy(PistonBlockSitStrategy, Int.MIN_VALUE)
        Sit.registerStrategy(SlabBlockSitStrategy, Int.MAX_VALUE - 1)
        Sit.registerStrategy(StairBlockSitStrategy, Int.MAX_VALUE - 1)
        Sit.registerStrategy(CampfireBlockSitStrategy, Int.MAX_VALUE - 1)
        Sit.registerStrategy(ScaffoldingBlockSitStrategy, Int.MAX_VALUE - 1)
        Sit.registerStrategy(DefaultBlockSitStrategy, Int.MAX_VALUE)
    }
}
