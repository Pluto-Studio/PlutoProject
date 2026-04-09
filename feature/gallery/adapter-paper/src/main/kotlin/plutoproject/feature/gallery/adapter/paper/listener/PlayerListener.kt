package plutoproject.feature.gallery.adapter.paper.listener

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object PlayerListener : Listener {
    @EventHandler
    suspend fun onItemFrameChange(event: PlayerItemFrameChangeEvent) {
        // TODO
    }
}
