package plutoproject.feature.velocity.joinQuitMessage

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "join_quit_message",
    platform = Platform.VELOCITY,
    dependencies = [Dependency("whitelist_v2", required = false)]
)
@Suppress("UNUSED")
class JoinQuitMessageFeature : VelocityFeature() {
    private val featureModule = module {
        single<JoinQuitMessageConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        server.eventManager.registerSuspend(plugin, JoinQuitListener)
    }
}
