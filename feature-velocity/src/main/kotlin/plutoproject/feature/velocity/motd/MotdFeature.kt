package plutoproject.feature.velocity.motd

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "motd",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class MotdFeature : VelocityFeature() {
    private val featureModule = module {
        single { MotdService(configFile = this@MotdFeature.saveConfig(), logger = this@MotdFeature.logger) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        server.eventManager.registerSuspend(plugin, MotdListener)
        AnnotationParser.parse(MotdCommand)
    }
}
