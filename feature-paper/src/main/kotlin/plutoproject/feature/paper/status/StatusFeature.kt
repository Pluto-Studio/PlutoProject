package plutoproject.feature.paper.status

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "status",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class StatusFeature : PaperFeature() {
    private val featureModule = module {
        single<StatusConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(StatusCommand)
        server.pluginManager.registerSuspendingEvents(StatusListener, plugin)
        MenuManager.registerButton(StatusButtonDescriptor) { Status() }
    }
}
