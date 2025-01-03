package ink.pmc.essentials.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import ink.pmc.essentials.TELEPORT_REQUEST_CANCELED_OFFLINE
import ink.pmc.essentials.TELEPORT_REQUEST_CANCELLED_SOUND
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.framework.chat.replace
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object TeleportListener : Listener, KoinComponent {

    private val manager by inject<TeleportManager>()

    @EventHandler
    suspend fun ServerTickEndEvent.e() {
        manager.tick()
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        val unfinished = manager.getUnfinishedRequest(player)
        val pending = manager.getPendingRequest(player)

        if (unfinished != null) {
            submitAsync { unfinished.cancel(false) }
            unfinished.destination.sendMessage(
                TELEPORT_REQUEST_CANCELED_OFFLINE
                    .replace("<player>", player.name)
            )
            unfinished.destination.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }

        if (pending != null) {
            submitAsync { pending.cancel() }
            pending.source.sendMessage(
                TELEPORT_REQUEST_CANCELED_OFFLINE
                    .replace("<player>", player.name)
            )
            pending.source.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }
    }

}