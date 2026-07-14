package plutoproject.feature.no_player_cap.paper

import io.papermc.paper.event.player.PlayerServerFullCheckEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.HandlerList
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "no_player_cap",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class NoPlayerCapFeature : RuntimeModule, Listener {
    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerEvents(this, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(this)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerServerFullCheckEvent.onServerFullCheck() {
        allow(true)
    }
}
