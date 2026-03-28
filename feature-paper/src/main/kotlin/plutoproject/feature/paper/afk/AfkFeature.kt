package plutoproject.feature.paper.afk

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.module
import plutoproject.feature.paper.api.afk.AfkManager
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

internal var disabled = true

@Feature(
    id = "afk",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class AfkFeature : PaperFeature() {
    private val featureModule = module {
        single<AfkConfig> { loadConfig(saveConfig()) }
        single<AfkManager> { AfkManagerImpl() }
    }

    override fun onEnable() {
        globalKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(AfkCommand)
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
        disabled = false
    }

    override fun onDisable() {
        disabled = true
    }
}
