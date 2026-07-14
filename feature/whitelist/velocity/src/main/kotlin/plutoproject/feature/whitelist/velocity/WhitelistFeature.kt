package plutoproject.feature.whitelist.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import net.luckperms.api.LuckPermsProvider
import org.koin.dsl.module
import plutoproject.capability.charonflow.api.CharonFlowConnection
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.capability.legacycloudcommands.api.velocity.VelocityLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.feature.whitelist.api.WhitelistService
import plutoproject.feature.whitelist.api.hook.WhitelistHookType
import plutoproject.feature.whitelist.common.commonModule
import plutoproject.feature.whitelist.velocity.commands.MigratorCommand
import plutoproject.feature.whitelist.velocity.commands.WhitelistCommand
import plutoproject.feature.whitelist.velocity.commands.WhitelistVisitorCommand
import plutoproject.feature.whitelist.velocity.listeners.PlayerListener
import plutoproject.feature.whitelist.velocity.listeners.VisitorListener
import plutoproject.foundation.velocity.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.velocity.VelocityModuleContext
import java.util.logging.Logger

@Feature(
    id = "whitelist",
    platform = Platform.VELOCITY,
    requiredCapabilities = ["mongo", "charonflow", "geoip", "profile", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class WhitelistFeature : RuntimeModule {
    private var playerListener: PlayerListener? = null
    private var visitorListener: VisitorListener? = null
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        context as VelocityModuleContext
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(WhitelistFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<WhitelistConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<CharonFlowConnection>()
        context.importServiceToKoin<GeoIpConnection>()
        context.importServiceToKoin<ProfileLookup>()
        context.loadKoinModuleDefinitions(
            module {
                single { config }
                single<Logger> { context.logger }
            },
            commonModule(context.coroutineScope),
        )

        context.services.exportServiceFromKoin<WhitelistService>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        val config = context.koinGet<WhitelistConfig>()
        if (config.visitorMode.enable && runCatching { LuckPermsProvider.get() }.isFailure) {
            error("访客模式功能已启用，但未找到 LuckPerms API")
        }

        VisitorState.setEnabled(config.visitorMode.enable)
        registerCommands(context, config)
        registerListeners(context)

        val service = context.koinGet<WhitelistService>()
        service.registerHook(WhitelistHookType.GrantWhitelist) { context.proxyServer.onWhitelistGrant(it) }
        service.registerHook(WhitelistHookType.RevokeWhitelist) { context.proxyServer.onWhitelistRevoke(it) }
    }

    private fun registerCommands(context: VelocityModuleContext, config: WhitelistConfig) {
        val parser = context.services.getService<VelocityLegacyCloudCommands>().parser
        val containers = buildList {
            add(WhitelistCommand)
            add(WhitelistVisitorCommand)
            if (config.enableMigrator) {
                add(MigratorCommand)
            }
        }

        commands = CloudCommandRegistration.register(parser, *containers.toTypedArray())
    }

    private fun registerListeners(context: VelocityModuleContext) {
        visitorListener = VisitorListener.also {
            context.proxyServer.eventManager.registerSuspend(context.pluginContainer, it)
        }
        playerListener = PlayerListener.also {
            context.proxyServer.eventManager.registerSuspend(context.pluginContainer, it)
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        context as VelocityModuleContext

        commands?.close()
        commands = null

        playerListener?.let { context.proxyServer.eventManager.unregisterListener(context.pluginContainer, it) }
        playerListener = null
        visitorListener?.let { context.proxyServer.eventManager.unregisterListener(context.pluginContainer, it) }
        visitorListener = null
        VisitorState.setEnabled(false)
    }
}
