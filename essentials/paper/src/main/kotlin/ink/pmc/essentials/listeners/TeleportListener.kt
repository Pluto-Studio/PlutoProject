package ink.pmc.essentials.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.TELEPORT_REQUEST_CANCELED_OFFLINE
import ink.pmc.essentials.TELEPORT_REQUEST_CANCELLED_SOUND
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.utils.chat.replace
import ink.pmc.utils.concurrent.submitSync
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object TeleportListener : Listener, KoinComponent {

    private val manager by inject<TeleportManager>()
    private val conf = get<EssentialsConfig>().Teleport()

    @EventHandler
    fun ServerTickEndEvent.e() {
        // 丢入休眠期间执行
        repeat(conf.queueProcessPerTick) {
            submitSync {
                manager.tick()
            }
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        val unfinished = manager.getUnfinishedRequest(player)
        val pending = manager.getPendingRequest(player)

        if (unfinished != null) {
            unfinished.cancel(false)
            unfinished.destination.sendMessage(
                TELEPORT_REQUEST_CANCELED_OFFLINE
                    .replace("<player>", player.name)
            )
            unfinished.destination.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }

        if (pending != null) {
            pending.cancel()
            pending.source.sendMessage(
                TELEPORT_REQUEST_CANCELED_OFFLINE
                    .replace("<player>", player.name)
            )
            pending.source.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }
    }

}