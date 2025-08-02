package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameters")
object PlayerListener : Listener, KoinComponent {
    private val exchangeShop by inject<InternalExchangeShop>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerJoinEvent.onPlayerJoin() {
        exchangeShop.coroutineScope.launch(Dispatchers.IO) {
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
