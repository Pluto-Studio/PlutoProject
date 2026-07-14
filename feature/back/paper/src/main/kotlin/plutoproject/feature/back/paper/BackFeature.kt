package plutoproject.feature.back.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.serveridentifier.api.ServerIdentifier
import plutoproject.feature.back.api.paper.BackManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "back",
    platform = Platform.PAPER,
    requiredFeatures = ["teleport"],
    requiredCapabilities = ["mongo", "server_identifier", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class BackFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(BackFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<BackConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<ServerIdentifier>()
        context.importServiceToKoin<TeleportManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single {
                val serverId = get<ServerIdentifier>().identifierOrThrow()
                BackRepository(get<MongoConnection>().getCollection<BackModel>("plutoproject_${serverId}_feature_back"))
            }
            single<BackManager> { BackManagerImpl() }
        })
        context.services.exportServiceFromKoin<BackManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, BackCommand)
        context.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(PlayerListener)
    }
}
