package plutoproject.feature.creeperfirework.paper

import org.bukkit.event.HandlerList
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "creeper_firework",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class CreeperFireworkFeature : RuntimeModule {
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerEvents(ExplosionListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(ExplosionListener)
    }
}
