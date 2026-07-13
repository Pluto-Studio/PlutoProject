package plutoproject.feature.farmprotection.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.HandlerList
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "farm_protection",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class FarmProtectionFeature : RuntimeModule {
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerSuspendingEvents(InteractionListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(InteractionListener)
    }
}
