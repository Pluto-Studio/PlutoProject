package plutoproject.feature.teleport.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.worldalias.api.worldalias.WorldAlias
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.teleport.paper.buttons.Teleport
import plutoproject.feature.teleport.paper.buttons.TeleportButtonDescriptor
import plutoproject.feature.teleport.paper.commands.TeleportCommons
import plutoproject.feature.teleport.paper.commands.TpaCommand
import plutoproject.feature.teleport.paper.commands.TpacceptCommand
import plutoproject.feature.teleport.paper.commands.TpcancelCommand
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "teleport",
    platform = Platform.PAPER,
    optionalFeatures = ["menu"],
    requiredCapabilities = ["interactive", "world_alias", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class TeleportFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(TeleportFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<TeleportConfig>()

        context.importServiceToKoin<GuiManager>()
        context.importServiceToKoin<WorldAlias>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<TeleportManager> { TeleportManagerImpl() }
        })
        context.services.exportServiceFromKoin<TeleportManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(
            parser,
            TeleportCommons,
            TpacceptCommand,
            TpaCommand,
            TpcancelCommand,
        )
        context.plugin.server.pluginManager.registerSuspendingEvents(TeleportListener, context.plugin)
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(TeleportButtonDescriptor) { Teleport() }
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(TeleportListener)
        context.koinGet<TeleportManager>().clearRequest()
    }
}
