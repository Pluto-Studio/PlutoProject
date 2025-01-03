package ink.pmc.essentials.listeners

import ink.pmc.essentials.api.afk.AfkManager
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object AfkListener : Listener, KoinComponent {
    private val manager by inject<AfkManager>()

    private fun Player.unAfk() {
        if (manager.isAfk(this)) {
            manager.toggle(this)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun PlayerMoveEvent.e() {
        if (!hasExplicitlyChangedBlock()) return
        player.unAfk()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun AsyncChatEvent.e() {
        if (isCancelled) return
        player.unAfk()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun PlayerTeleportEvent.e() {
        if (isCancelled) return
        player.unAfk()
    }
}