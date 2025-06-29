package plutoproject.feature.paper.home

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mojang.brigadier.arguments.StringArgumentType
import io.leangen.geantyref.TypeToken
import org.bukkit.command.CommandSender
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.koin.dsl.module
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.home.commands.*
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.provider.getCollection
import plutoproject.framework.common.util.command.getKotlinMethodArgumentParserClass
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.serverName
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.command.CommandManager
import plutoproject.framework.paper.util.command.PaperPrivilegedSuggestion
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

internal var disabled = true

@Feature(
    id = "home",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "teleport", load = Load.BEFORE, required = true),
        Dependency(id = "menu", load = Load.BEFORE, required = false),
    ],
)
@Suppress("UNUSED")
class HomeFeature : PaperFeature() {
    private val featureModule = module {
        single<HomeConfig> { loadConfig(saveConfig()) }
        single<HomeRepository> { HomeRepository(Provider.getCollection<HomeModel>("essentials_${serverName}_homes")) }
        single<HomeManager> { HomeManagerImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
        CommandManager.parserRegistry().apply {
            registerSuggestionProvider(
                "homes-offlineplayer",
                PaperPrivilegedSuggestion.of(OfflinePlayerParser(), HOME_LOOKUP_OTHER_PERMISSION)
            )
        }
        CommandManager.brigadierManager().apply {
            registerMapping(TypeToken.get(CommandManager.getKotlinMethodArgumentParserClass<CommandSender, Home>())) {
                it.cloudSuggestions().to { StringArgumentType.greedyString() }
            }
        }
        AnnotationParser.parse(
            HomeCommons,
            DelHomeCommand,
            HomeCommand,
            HomesCommand,
            SetHomeCommand
        )
        if (isMenuAvailable) {
            MenuManager.registerButton(HomeButtonDescriptor) { Home() }
        }
        disabled = false
    }

    override fun onDisable() {
        disabled = true
    }
}
