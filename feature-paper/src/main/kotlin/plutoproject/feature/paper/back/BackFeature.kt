package plutoproject.feature.paper.back

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.module
import plutoproject.feature.paper.api.back.BackManager
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
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "back",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "teleport", load = Load.BEFORE, required = true),
    ],
)
@Suppress("UNUSED")
class BackFeature : PaperFeature() {
    private val featureModule = module {
        single<BackConfig> { loadConfig(saveConfig()) }
        single<BackRepository> { BackRepository(Provider.getCollection<BackModel>("essentials_${serverName}_backs")) }
        single<BackManager> { BackManagerImpl() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(BackCommand)
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
    }
}
