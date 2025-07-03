package plutoproject.feature.paper.sit

import org.koin.dsl.module
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.sit.block.BlockSitImpl
import plutoproject.feature.paper.sit.block.SitCommand
import plutoproject.feature.paper.sit.block.listeners.BlockSitBlockListener
import plutoproject.feature.paper.sit.block.listeners.BlockSitChunkListener
import plutoproject.feature.paper.sit.block.listeners.BlockSitPlayerListener
import plutoproject.feature.paper.sit.block.listeners.BlockSitServerListener
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
        single<BlockSit> { BlockSitImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        initializeBlockSit()
    }

    private fun initializeBlockSit() {
        AnnotationParser.parse(SitCommand)
        server.pluginManager.registerEvents(BlockSitServerListener, plugin)
        server.pluginManager.registerEvents(BlockSitChunkListener, plugin)
        server.pluginManager.registerEvents(BlockSitPlayerListener, plugin)
        server.pluginManager.registerEvents(BlockSitBlockListener, plugin)
    }
}
