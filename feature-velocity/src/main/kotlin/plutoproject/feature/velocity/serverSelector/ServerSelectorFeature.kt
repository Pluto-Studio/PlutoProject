package plutoproject.feature.velocity.serverSelector

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import org.koin.dsl.module
import plutoproject.feature.velocity.serverSelector.commands.LobbyCommand
import plutoproject.feature.velocity.serverSelector.listeners.AutoJoinListener
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "server_selector",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class ServerSelectorFeature : VelocityFeature() {
    private val velocityFeatureModule = module {
        single<VelocityServerSelectorConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(velocityFeatureModule)
        }
        AnnotationParser.parse(LobbyCommand)
        server.eventManager.registerSuspend(plugin, AutoJoinListener)
    }
}
