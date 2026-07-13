package plutoproject.feature.randomteleport.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.incendo.cloud.bukkit.parser.WorldParser
import org.koin.dsl.module
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.randomteleport.api.paper.RandomTeleportManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.randomteleport.paper.config.BiomeDecoder
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.foundation.paper.command.withPermission
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "random_teleport",
    platform = Platform.PAPER,
    requiredFeatures = ["teleport"],
    optionalFeatures = ["menu"],
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class RandomTeleportFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(RandomTeleportFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addDecoder(BiomeDecoder)
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<RandomTeleportConfig>()

        context.importServiceToKoin<TeleportManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<RandomTeleportManager> { RandomTeleportManagerImpl() }
        })
        context.services.exportServiceFromKoin<RandomTeleportManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        parser.manager().parserRegistry().registerSuggestionProvider(
            "rtp-world",
            WorldParser<org.bukkit.command.CommandSender>().withPermission(RANDOM_TELEPORT_SPECIFIC_PERMISSION),
        )
        commands = CloudCommandRegistration.register(parser, RtpCommand)
        context.plugin.server.pluginManager.registerSuspendingEvents(RandomTeleportListener, context.plugin)
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(RandomTeleportButtonDescriptor) { RandomTeleport() }
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(RandomTeleportListener)
    }
}
