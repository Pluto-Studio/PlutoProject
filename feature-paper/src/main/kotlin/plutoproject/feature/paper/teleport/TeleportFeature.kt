package plutoproject.feature.paper.teleport

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.feature.paper.teleport.buttons.Teleport
import plutoproject.feature.paper.teleport.buttons.TeleportButtonDescriptor
import plutoproject.feature.paper.teleport.commands.TeleportCommons
import plutoproject.feature.paper.teleport.commands.TpaCommand
import plutoproject.feature.paper.teleport.commands.TpacceptCommand
import plutoproject.feature.paper.teleport.commands.TpcancelCommand
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "teleport",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "menu", load = Load.BEFORE, required = false),
    ],
)
@Suppress("UNUSED")
class TeleportFeature : PaperFeature() {
    private val featureModule = module {
        single<TeleportConfig> { loadConfig(saveConfig()) }
        single<TeleportManager> { TeleportManagerImpl() }
    }

    override fun onEnable() {
        globalKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(
            TeleportCommons,
            TpacceptCommand,
            TpaCommand,
            TpcancelCommand
        )
        server.pluginManager.registerSuspendingEvents(TeleportListener, plugin)
        if (isMenuAvailable) {
            MenuManager.registerButton(TeleportButtonDescriptor) { Teleport() }
        }
    }

    override fun onDisable() {
        TeleportManager.clearRequest()
    }
}
