package plutoproject.feature.dev_watermark.paper

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

@Suppress("UnusedReceiverParameter")
object TickListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        val server = (currentModuleContext() as PaperModuleContext).plugin.server
        server.onlinePlayers.forEach { player ->
            player.sendActionBar(DEV_WATERMARK)
        }
    }
}
