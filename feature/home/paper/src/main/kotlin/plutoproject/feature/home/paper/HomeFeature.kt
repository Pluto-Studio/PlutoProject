package plutoproject.feature.home.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mojang.brigadier.arguments.StringArgumentType
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import io.leangen.geantyref.TypeToken
import org.bukkit.command.CommandSender
import org.bukkit.event.HandlerList
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.ArgumentParser
import org.koin.dsl.module
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.capability.serveridentifier.api.ServerIdentifier
import plutoproject.capability.worldalias.api.worldalias.WorldAlias
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.commands.*
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.foundation.paper.command.withPermission
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

internal var disabled = true

@Feature(
    id = "home",
    platform = Platform.PAPER,
    requiredFeatures = ["teleport"],
    optionalFeatures = ["menu"],
    requiredCapabilities = [
        "mongo",
        "geoip",
        "server_identifier",
        "profile",
        "interactive",
        "world_alias",
        "legacy_cloud_commands",
    ],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class HomeFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(HomeFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<HomeConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<GeoIpConnection>()
        context.importServiceToKoin<ServerIdentifier>()
        context.importServiceToKoin<ProfileLookup>()
        context.importServiceToKoin<GuiManager>()
        context.importServiceToKoin<WorldAlias>()
        context.importServiceToKoin<TeleportManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single {
                val serverId = get<ServerIdentifier>().identifierOrThrow()
                HomeRepository(get<MongoConnection>().getCollection<HomeModel>("plutoproject_${serverId}_feature_home"))
            }
            single<HomeManager> { HomeManagerImpl() }
        })
        context.services.exportServiceFromKoin<HomeManager>()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        val manager = parser.manager() as LegacyPaperCommandManager<CommandSender>
        manager.parserRegistry().registerSuggestionProvider(
            "homes-offlineplayer",
            OfflinePlayerParser<CommandSender>().withPermission(HOME_LOOKUP_OTHER_PERMISSION),
        )
        val parserClass = Class.forName(
            "org.incendo.cloud.kotlin.coroutines.annotations.KotlinMethodArgumentParser"
        ) as Class<ArgumentParser<CommandSender, Home>>
        manager.brigadierManager().registerMapping(TypeToken.get(parserClass)) {
            it.cloudSuggestions().to { StringArgumentType.greedyString() }
        }
        commands = CloudCommandRegistration.register(
            parser,
            HomeCommons,
            DelHomeCommand,
            HomeCommand,
            HomesCommand,
            SetHomeCommand,
        )
        context.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, context.plugin)
        context.services.getServiceOrNull<MenuManager>()?.registerButton(HomeButtonDescriptor) { Home() }
        disabled = false
    }

    override suspend fun onDisable(context: ModuleContext) {
        disabled = true
        commands?.close()
        commands = null
        HandlerList.unregisterAll(PlayerListener)
    }
}
