package plutoproject.feature.paper.warp

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mojang.brigadier.arguments.StringArgumentType
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.minimessage.MiniMessage
import org.incendo.cloud.minecraft.extras.parser.ComponentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.feature.paper.warp.buttons.Spawn
import plutoproject.feature.paper.warp.buttons.SpawnButtonDescriptor
import plutoproject.feature.paper.warp.buttons.Warp
import plutoproject.feature.paper.warp.buttons.WarpButtonDescriptor
import plutoproject.feature.paper.warp.commands.*
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.provider.getCollection
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.serverName
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.command.CommandManager
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "warp",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "teleport", load = Load.BEFORE, required = true),
        Dependency(id = "menu", load = Load.BEFORE, required = false),
    ],
)
@Suppress("UNUSED")
class WarpFeature : PaperFeature() {
    private val featureModule = module {
        single<WarpConfig> { loadConfig(saveConfig()) }
        single<WarpRepository> { WarpRepository(Provider.getCollection("essentials_${serverName}_warps")) }
        single<WarpManager> { WarpManagerImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        CommandManager.parserRegistry().apply {
            registerSuggestionProvider(
                "warps",
                WarpParser(false)
            )
            registerSuggestionProvider(
                "warps-without-alias",
                WarpParser(true)
            )
            registerNamedParser(
                "warp",
                ParserDescriptor.of(WarpParser(false), Warp::class.java)
            )
            registerNamedParser(
                "warp-without-alias",
                ParserDescriptor.of(WarpParser(true), Warp::class.java)
            )
            registerNamedParser(
                "spawn",
                ParserDescriptor.of(SpawnParser(), Warp::class.java)
            )
            registerNamedParser(
                "editwarp-component",
                ComponentParser.componentParser(MiniMessage.miniMessage(), StringParser.StringMode.QUOTED)
            )
        }
        CommandManager.brigadierManager().apply {
            registerMapping(TypeToken.get(WarpParser::class.java)) {
                it.cloudSuggestions().to { parser ->
                    if (!parser.withoutAlias) StringArgumentType.greedyString() else StringArgumentType.string()
                }
            }
            registerMapping(TypeToken.get(SpawnParser::class.java)) {
                it.cloudSuggestions().to { StringArgumentType.greedyString() }
            }
        }
        AnnotationParser.parse(
            WarpCommons,
            DelWarpCommand,
            EditWarpCommand,
            PreferredSpawnCommand,
            SetWarpCommand,
            SpawnCommand,
            WarpCommand,
            WarpsCommand
        )
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
        if (isMenuAvailable) {
            MenuManager.registerButton(WarpButtonDescriptor) { Warp() }
            MenuManager.registerButton(SpawnButtonDescriptor) { Spawn() }
        }
    }
}
