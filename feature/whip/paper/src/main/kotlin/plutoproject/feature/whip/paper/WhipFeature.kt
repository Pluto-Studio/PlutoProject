package plutoproject.feature.whip.paper

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.koinInject
import plutoproject.kernel.api.loadKoinModuleDefinitions
import plutoproject.kernel.api.paper.PaperModuleContext
import org.koin.dsl.module

@Feature(
    id = "whip",
    platform = Platform.PAPER,
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class WhipFeature : RuntimeModule {
    private val config by koinInject<WhipConfig>()
    private var commands: CloudCommandRegistration? = null
    private var sessionManager: WhipSessionManager? = null
    private var registeredListeners: List<Listener> = emptyList()

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val loadedConfig = ConfigLoaderBuilder.empty()
            .withClassLoader(WhipFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<WhipConfig>()
        context.loadKoinModuleDefinitions(module { single { loadedConfig } })
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        registerWhipRecipes(context.plugin.server, config)
        val sessionManager = WhipSessionManager(context.coroutineScope, config)
        val craftListener = WhipCraftListener(config)
        val interactionListener = WhipInteractionListener(sessionManager)
        context.plugin.server.pluginManager.registerEvents(craftListener, context.plugin)
        context.plugin.server.pluginManager.registerEvents(interactionListener, context.plugin)
        this.sessionManager = sessionManager
        registeredListeners = listOf(craftListener, interactionListener)
        context.plugin.server.onlinePlayers.forEach { it.discoverRecipes(WHIP_RECIPE_KEYS) }

        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, WhipCommand(config))
    }

    override suspend fun onDisable(context: ModuleContext) {
        val sessionManager = this.sessionManager
        this.sessionManager = null
        sessionManager?.stopAll()

        commands?.close()
        commands = null
        registeredListeners.forEach(HandlerList::unregisterAll)
        registeredListeners = emptyList()
        unregisterWhipRecipes((context as PaperModuleContext).plugin.server)
    }
}
