package plutoproject.feature.paper.dev_watermark

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.framework.paper.util.server

@Suppress("UnusedReceiverParameter")
object TickListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        server.onlinePlayers.forEach { player ->
            player.sendActionBar(DEV_WATERMARK)
        }
    }
}
