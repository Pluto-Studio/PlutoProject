package plutoproject.feature.paper.exchangeshop

import org.bukkit.Bukkit
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
    suspend fun PlayerJoinEvent.onPlayerJoin() {
        Bukkit.shutdown()
        exchangeShop.getUserOrCreate(player)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerQuitEvent.onPlayerQuit() {
        exchangeShop.unloadUser(player.uniqueId)
    }
}
