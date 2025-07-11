package plutoproject.feature.paper.serverSelector

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.serverSelector.button.ServerSelector
import plutoproject.feature.paper.serverSelector.button.ServerSelectorButtonDescriptor
import plutoproject.feature.paper.serverSelector.listeners.LobbyListener
import plutoproject.feature.paper.serverSelector.listeners.TimeSyncListener
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger

internal lateinit var featureLogger: Logger

@Feature(
    id = "server_selector",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "menu", load = Load.BEFORE, required = false),
    ],
)
@Suppress("UNUSED")
class ServerSelectorFeature : PaperFeature() {
    private val bukkitFeatureModule = module {
        single<ServerSelectorConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        featureLogger = logger
        configureKoin {
            modules(bukkitFeatureModule)
        }
        loadLobbyWorld()
        if (isMenuAvailable) {
            MenuManager.registerButton(ServerSelectorButtonDescriptor) { ServerSelector() }
        }
        server.pluginManager.registerSuspendingEvents(LobbyListener, plugin)
        server.pluginManager.registerSuspendingEvents(TimeSyncListener, plugin)
    }

    override fun onDisable() {
        stopTimeSyncJobs()
    }
}
