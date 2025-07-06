package plutoproject.feature.paper.sit

import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause
import plutoproject.feature.paper.sit.block.BlockSitImpl
import plutoproject.feature.paper.sit.block.InternalBlockSit
import plutoproject.feature.paper.sit.block.SitCommand
import plutoproject.feature.paper.sit.block.listeners.*
import plutoproject.feature.paper.sit.player.InternalPlayerSit
import plutoproject.feature.paper.sit.player.PlayerSitImpl
import plutoproject.feature.paper.sit.player.PlayerSitOptionDescriptor
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.api.options.OptionsManager
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
        single { BlockSitImpl() } binds arrayOf(BlockSit::class, InternalBlockSit::class)
        single { PlayerSitImpl() } binds arrayOf(PlayerSit::class, InternalPlayerSit::class)
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        initializeBlockSit()
        initializePlayerSit()
    }

    override fun onDisable() {
        shutdownBlockSit()
        shutdownPlayerSit()
    }

    private fun initializeBlockSit() {
        AnnotationParser.parse(SitCommand)
        server.pluginManager.registerEvents(ServerListener, plugin)
        server.pluginManager.registerEvents(ChunkListener, plugin)
        server.pluginManager.registerEvents(PlayerListener, plugin)
        server.pluginManager.registerEvents(BlockListener, plugin)
        server.pluginManager.registerEvents(EntityListener, plugin)
    }

    private fun initializePlayerSit() {
        OptionsManager.registerOptionDescriptor(PlayerSitOptionDescriptor)
    }

    private fun shutdownBlockSit() {
        BlockSit.sitters.forEach {
            BlockSit.standUp(it, StandUpFromBlockCause.FEATURE_DISABLE)
        }
    }

    private fun shutdownPlayerSit() {
        PlayerSit.stacks.forEach {
            PlayerSit.destroyStack(it, cause = PlayerStackDestroyCause.FEATURE_DISABLE)
        }
    }
}
