package plutoproject.feature.paper.teleport

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.PluginScope

@Suppress("UNUSED", "UnusedReceiverParameter")
object TeleportListener : Listener, KoinComponent {
    @EventHandler
    fun ServerTickEndEvent.e() {
        PluginScope.launch {
            TeleportManager.tick()
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        val unfinished = TeleportManager.getUnfinishedRequest(player)
        val pending = TeleportManager.getPendingRequest(player)

        if (unfinished != null) {
            PluginScope.launch {
                unfinished.cancel(false)
            }
            unfinished.destination.sendMessage(TELEPORT_REQUEST_CANCELED_OFFLINE.replace("<player>", player.name))
            unfinished.destination.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }

        if (pending != null) {
            PluginScope.launch {
                pending.cancel()
            }
            pending.source.sendMessage(TELEPORT_REQUEST_CANCELED_OFFLINE.replace("<player>", player.name))
            pending.source.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }
    }
}
