package plutoproject.feature.status.paper

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.menu.api.paper.factory.ButtonDescriptorFactory
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "status",
    platform = Platform.PAPER,
    requiredFeatures = ["menu"],
    requiredCapabilities = ["geoip", "server_statistics", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class StatusFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(StatusFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<StatusConfig>()
        context.loadKoinModuleDefinitions(module { single { config } })
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, StatusCommand)
        context.plugin.server.pluginManager.registerEvents(StatusListener, context.plugin)

        val descriptor = context.services.getService<ButtonDescriptorFactory>().create("status:status")
        context.services.getService<MenuManager>().registerButton(descriptor) { Status() }
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(StatusListener)
    }
}
