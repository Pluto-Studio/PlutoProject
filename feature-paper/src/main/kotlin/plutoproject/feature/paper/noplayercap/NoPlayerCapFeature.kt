package plutoproject.feature.paper.noplayercap

import io.papermc.paper.event.player.PlayerServerFullCheckEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "no_player_cap",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class NoPlayerCapFeature : PaperFeature(), Listener {
    override fun onEnable() {
        server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerServerFullCheckEvent.onServerFullCheck() {
        allow(true)
    }
}
