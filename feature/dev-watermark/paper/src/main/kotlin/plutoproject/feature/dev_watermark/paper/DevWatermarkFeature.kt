package plutoproject.feature.dev_watermark.paper

import org.bukkit.event.HandlerList
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "dev_watermark",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class DevWatermarkFeature : RuntimeModule {
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerEvents(TickListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(TickListener)
    }
}
