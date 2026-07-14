package plutoproject.feature.sit.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.HandlerList
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.sit.api.paper.block.BlockSit
import plutoproject.feature.sit.api.paper.block.StandUpFromBlockCause
import plutoproject.feature.sit.api.paper.player.PlayerSit
import plutoproject.feature.sit.api.paper.player.PlayerStackDestroyCause
import plutoproject.feature.sit.paper.block.BlockSitImpl
import plutoproject.feature.sit.paper.block.InternalBlockSit
import plutoproject.feature.sit.paper.block.SitCommand
import plutoproject.feature.sit.paper.block.listeners.*
import plutoproject.feature.sit.paper.player.*
import plutoproject.feature.sit.paper.player.listeners.PlayerSitChunkListener
import plutoproject.feature.sit.paper.player.listeners.PlayerSitEntityListener
import plutoproject.feature.sit.paper.player.listeners.PlayerSitPlayerListener
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "sit",
    platform = Platform.PAPER,
    optionalFeatures = ["menu"],
    requiredCapabilities = ["database_persist", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
class SitFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null
    private val blockSit by koinInject<BlockSit>()
    private val playerSit by koinInject<PlayerSit>()

    override suspend fun onLoad(context: ModuleContext) {
        val databasePersist = context.services.getService<DatabasePersist>()
        context.loadKoinModuleDefinitions(module {
            single { databasePersist }
            single { BlockSitImpl() } binds arrayOf(BlockSit::class, InternalBlockSit::class)
            single { PlayerSitImpl() } binds arrayOf(PlayerSit::class, InternalPlayerSit::class)
        })
        context.services.exportServiceFromKoin<BlockSit>()
        context.services.exportServiceFromKoin<PlayerSit>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        initializeBlockSit(context)
        initializePlayerSit(context)
        context.plugin.server.pluginManager.registerEvents(CommonServerListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        shutdownBlockSit()
        shutdownPlayerSit()
        HandlerList.unregisterAll(CommonServerListener)
        HandlerList.unregisterAll(BlockSitServerListener)
        HandlerList.unregisterAll(BlockSitChunkListener)
        HandlerList.unregisterAll(BlockSitPlayerListener)
        HandlerList.unregisterAll(BlockSitBlockListener)
        HandlerList.unregisterAll(BlockSitEntityListener)
        HandlerList.unregisterAll(PlayerSitChunkListener)
        HandlerList.unregisterAll(PlayerSitEntityListener)
        HandlerList.unregisterAll(PlayerSitPlayerListener)
    }

    private fun initializeBlockSit(context: PaperModuleContext) {
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, SitCommand)
        context.plugin.server.pluginManager.registerEvents(BlockSitServerListener, context.plugin)
        context.plugin.server.pluginManager.registerEvents(BlockSitChunkListener, context.plugin)
        context.plugin.server.pluginManager.registerEvents(BlockSitPlayerListener, context.plugin)
        context.plugin.server.pluginManager.registerEvents(BlockSitBlockListener, context.plugin)
        context.plugin.server.pluginManager.registerEvents(BlockSitEntityListener, context.plugin)
    }

    private fun initializePlayerSit(context: PaperModuleContext) {
        context.plugin.server.pluginManager.registerEvents(PlayerSitChunkListener, context.plugin)
        context.plugin.server.pluginManager.registerEvents(PlayerSitEntityListener, context.plugin)
        context.plugin.server.pluginManager.registerSuspendingEvents(PlayerSitPlayerListener, context.plugin)
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(PlayerSitFeatureButtonDescriptor) { PlayerSitToggle() }
    }

    private fun shutdownBlockSit() {
        blockSit.sitters.forEach {
            blockSit.standUp(it, StandUpFromBlockCause.FEATURE_DISABLE)
        }
    }

    private fun shutdownPlayerSit() {
        playerSit.stacks.forEach {
            playerSit.destroyStack(it, cause = PlayerStackDestroyCause.FEATURE_DISABLE)
        }
    }
}
