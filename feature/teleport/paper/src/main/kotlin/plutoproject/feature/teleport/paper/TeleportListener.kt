package plutoproject.feature.teleport.paper

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.kernel.api.koinInject
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.foundation.common.text.replace
import plutoproject.feature.teleport.paper.moduleScope

@Suppress("UNUSED", "UnusedReceiverParameter")
object TeleportListener : Listener {
    @EventHandler
    fun ServerTickEndEvent.e() {
        moduleScope.launch {
            teleportManager.tick()
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        val unfinished = teleportManager.getUnfinishedRequest(player)
        val pending = teleportManager.getPendingRequest(player)

        if (unfinished != null) {
            moduleScope.launch {
                unfinished.cancel(false)
            }
            unfinished.destination.sendMessage(TELEPORT_REQUEST_CANCELED_OFFLINE.replace("<player>", player.name))
            unfinished.destination.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }

        if (pending != null) {
            moduleScope.launch {
                pending.cancel()
            }
            pending.source.sendMessage(TELEPORT_REQUEST_CANCELED_OFFLINE.replace("<player>", player.name))
            pending.source.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }
    }
}
