package ink.pmc.essentials.listeners

import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object WarpListener : Listener, KoinComponent {
    private val manager by inject<WarpManager>()

    @EventHandler
    fun PlayerJoinEvent.e() {
        submitAsync {
            if (manager.getPreferredSpawn(player) != null) return@submitAsync
            val default = manager.getDefaultSpawn() ?: return@submitAsync
            manager.setPreferredSpawn(player, default)
        }
    }
}