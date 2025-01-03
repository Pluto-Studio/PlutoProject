package ink.pmc.essentials.listeners

import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.concurrent.submitAsync
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED", "UnusedReceiverParameter")
object HomeListener : Listener, KoinComponent {
    private val manager by inject<HomeManager>()

    @EventHandler
    fun PlayerJoinEvent.e() {
        submitAsync {
            // 加载所有家
            manager.list(player)
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        manager.unloadAll(player)
    }
}