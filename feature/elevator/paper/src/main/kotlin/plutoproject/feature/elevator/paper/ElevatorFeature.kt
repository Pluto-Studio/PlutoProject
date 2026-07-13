package plutoproject.feature.elevator.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.feature.elevator.api.paper.ElevatorManager
import plutoproject.feature.elevator.paper.builders.IronElevatorBuilder
import plutoproject.feature.elevator.paper.listeners.ElevatorListener
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "elevator",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class ElevatorFeature : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        context.loadKoinModuleDefinitions(module {
            single<ElevatorManager> {
                ElevatorManagerImpl().also { it.registerBuilder(IronElevatorBuilder) }
            }
        })
        context.services.exportServiceFromKoin<ElevatorManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerSuspendingEvents(ElevatorListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(ElevatorListener)
    }
}
