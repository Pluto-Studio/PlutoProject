package plutoproject.feature.paper.dynamicScheduler

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.paper.api.dynamicScheduler.DynamicScheduler
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.dynamicScheduler.buttons.ViewBoost
import plutoproject.feature.paper.dynamicScheduler.buttons.ViewBoostButtonDescriptor
import plutoproject.feature.paper.dynamicScheduler.config.DynamicSchedulerConfig
import plutoproject.feature.paper.dynamicScheduler.listeners.DynamicViewDistanceListener
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.api.statistic.StatisticProvider
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger

var disabled = true
internal lateinit var featureLogger: Logger

@Feature(
    id = "dynamic_scheduler",
    platform = Platform.PAPER,
    dependencies = [
        Dependency(id = "menu", load = Load.BEFORE, required = false),
    ],
)
@Suppress("UNUSED")
class DynamicSchedulerFeature : PaperFeature(), KoinComponent {
    private val config by inject<DynamicSchedulerConfig>()
    private val featureModule = module {
        single<DynamicSchedulerConfig> { loadConfig(saveConfig()) }
        single<DynamicScheduler> { DynamicSchedulerImpl() }
    }

    override fun onEnable() {
        featureLogger = logger
        configureKoin {
            modules(featureModule)
        }
        logger.info("Using statistic provider: ${StatisticProvider.type}")
        AnnotationParser.parse(plutoproject.feature.paper.dynamicScheduler.commands.DynamicSchedulerCommand)
        if (isMenuAvailable) {
            MenuManager.registerButton(ViewBoostButtonDescriptor) { ViewBoost() }
        }
        server.pluginManager.registerSuspendingEvents(DynamicViewDistanceListener, plugin)
        DynamicScheduler.start()
    }

    override fun onDisable() {
        DynamicScheduler.stop()
        disabled = true
    }
}
