package plutoproject.feature.no_join_quit_message.paper

import org.bukkit.event.HandlerList
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "no_join_quit_message",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class NoJoinQuitMessageFeature : RuntimeModule {
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerEvents(NoJoinQuitMessageListener, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(NoJoinQuitMessageListener)
    }
}
