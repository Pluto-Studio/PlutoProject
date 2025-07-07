package plutoproject.feature.paper.sit

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause
import plutoproject.feature.paper.sit.block.BlockSitImpl
import plutoproject.feature.paper.sit.block.InternalBlockSit
import plutoproject.feature.paper.sit.block.SitCommand
import plutoproject.feature.paper.sit.block.listeners.*
import plutoproject.feature.paper.sit.player.*
import plutoproject.feature.paper.sit.player.listeners.PlayerSitChunkListener
import plutoproject.feature.paper.sit.player.listeners.PlayerSitEntityListener
import plutoproject.feature.paper.sit.player.listeners.PlayerSitPlayerListener
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
        server.pluginManager.registerEvents(CommonServerListener, plugin)
    }

    override fun onDisable() {
        shutdownBlockSit()
        shutdownPlayerSit()
    }

    private fun initializeBlockSit() {
        AnnotationParser.parse(SitCommand)
        server.pluginManager.registerEvents(BlockSitServerListener, plugin)
        server.pluginManager.registerEvents(BlockSitChunkListener, plugin)
        server.pluginManager.registerEvents(BlockSitPlayerListener, plugin)
        server.pluginManager.registerEvents(BlockSitBlockListener, plugin)
        server.pluginManager.registerEvents(BlockSitEntityListener, plugin)
    }

    private fun initializePlayerSit() {
        server.pluginManager.registerEvents(PlayerSitChunkListener, plugin)
        server.pluginManager.registerEvents(PlayerSitEntityListener, plugin)
        server.pluginManager.registerSuspendingEvents(PlayerSitPlayerListener, plugin)
        OptionsManager.registerOptionDescriptor(PlayerSitOptionDescriptor)
        if (isMenuAvailable) {
            MenuManager.registerButton(PlayerSitFeatureButtonDescriptor) { PlayerSitToggle() }
        }
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
