package plutoproject.feature.warp.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mojang.brigadier.arguments.StringArgumentType
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.event.HandlerList
import org.incendo.cloud.minecraft.extras.parser.ComponentParser
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.capability.serveridentifier.api.ServerIdentifier
import plutoproject.capability.worldalias.api.worldalias.WorldAlias
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.feature.warp.paper.buttons.Spawn
import plutoproject.feature.warp.paper.buttons.SpawnButtonDescriptor
import plutoproject.feature.warp.paper.buttons.WarpButtonDescriptor
import plutoproject.feature.warp.paper.commands.*
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "warp",
    platform = Platform.PAPER,
    requiredFeatures = ["teleport"],
    optionalFeatures = ["menu"],
    requiredCapabilities = [
        "mongo",
        "geoip",
        "server_identifier",
        "database_persist",
        "profile",
        "interactive",
        "world_alias",
        "legacy_cloud_commands",
    ],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class WarpFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(WarpFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<WarpConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<GeoIpConnection>()
        context.importServiceToKoin<ServerIdentifier>()
        context.importServiceToKoin<DatabasePersist>()
        context.importServiceToKoin<ProfileLookup>()
        context.importServiceToKoin<GuiManager>()
        context.importServiceToKoin<WorldAlias>()
        context.importServiceToKoin<TeleportManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single {
                val serverId = get<ServerIdentifier>().identifierOrThrow()
                WarpRepository(get<MongoConnection>().getCollection<WarpModel>("essentials_${serverId}_warps"))
            }
            single<WarpManager> { WarpManagerImpl() }
        })
        context.services.exportServiceFromKoin<WarpManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        val manager = parser.manager() as LegacyPaperCommandManager<CommandSender>
        manager.parserRegistry().apply {
            registerSuggestionProvider("warps", WarpParser(false))
            registerSuggestionProvider("warps-without-alias", WarpParser(true))
            registerNamedParser("warp", ParserDescriptor.of(WarpParser(false), Warp::class.java))
            registerNamedParser("warp-without-alias", ParserDescriptor.of(WarpParser(true), Warp::class.java))
            registerNamedParser("spawn", ParserDescriptor.of(SpawnParser(), Warp::class.java))
            registerNamedParser(
                "editwarp-component",
                ComponentParser.componentParser(MiniMessage.miniMessage(), StringParser.StringMode.QUOTED),
            )
        }
        manager.brigadierManager().apply {
            registerMapping(TypeToken.get(WarpParser::class.java)) {
                it.cloudSuggestions().to { warpParser ->
                    if (!warpParser.withoutAlias) StringArgumentType.greedyString() else StringArgumentType.string()
                }
            }
            registerMapping(TypeToken.get(SpawnParser::class.java)) {
                it.cloudSuggestions().to { StringArgumentType.greedyString() }
            }
        }
        commands = CloudCommandRegistration.register(
            parser,
            WarpCommons,
            DelWarpCommand,
            EditWarpCommand,
            PreferredSpawnCommand,
            SetWarpCommand,
            SpawnCommand,
            WarpCommand,
            WarpsCommand,
        )
        context.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, context.plugin)
        context.services.getServiceOrNull<MenuManager>()?.let { menu ->
            menu.registerButton(WarpButtonDescriptor) { plutoproject.feature.warp.paper.buttons.Warp() }
            menu.registerButton(SpawnButtonDescriptor) { Spawn() }
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(PlayerListener)
    }
}
