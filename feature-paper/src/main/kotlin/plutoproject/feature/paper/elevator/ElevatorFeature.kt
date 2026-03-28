package plutoproject.feature.paper.elevator

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.module
import plutoproject.feature.paper.api.elevator.ElevatorManager
import plutoproject.feature.paper.elevator.builders.IronElevatorBuilder
import plutoproject.feature.paper.elevator.listeners.ElevatorListener
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "elevator",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class ElevatorFeature : PaperFeature() {
    private val featureModule = module {
        single<ElevatorManager> { ElevatorManagerImpl() }
    }

    override fun onEnable() {
        globalKoin {
            modules(featureModule)
        }
        server.pluginManager.registerSuspendingEvents(ElevatorListener, plugin)
        ElevatorManager.registerBuilder(IronElevatorBuilder)
    }
}
