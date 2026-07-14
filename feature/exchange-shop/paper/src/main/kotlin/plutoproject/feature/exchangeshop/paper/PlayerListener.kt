package plutoproject.feature.exchangeshop.paper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.kernel.api.koinInject
import plutoproject.foundation.common.coroutine.Loom

@Suppress("UNUSED", "UnusedReceiverParameters")
object PlayerListener : Listener {
    private val exchangeShop by koinInject<InternalExchangeShop>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerJoinEvent.onPlayerJoin() {
        exchangeShop.coroutineScope.launch(Dispatchers.Loom) {
            exchangeShop.getUserOrCreate(player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerQuitEvent.onPlayerQuit() {
        exchangeShop.coroutineScope.launch {
            exchangeShop.unloadUser(player.uniqueId)
        }
    }
}
