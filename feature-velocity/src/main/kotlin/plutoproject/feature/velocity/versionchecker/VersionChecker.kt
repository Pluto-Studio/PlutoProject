package plutoproject.feature.velocity.versionchecker

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

@Feature(
    id = "version_checker",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class VersionChecker : VelocityFeature() {
    private val featureModule = module {
        single<VersionCheckerConfig> {
            loadConfig(saveConfig()) {
                addDecoder(IntRangeDecoder)
            }
        }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        server.eventManager.registerSuspend(plugin, PingListener)
        AnnotationParser.parse(IgnoreCommand)
    }
}
