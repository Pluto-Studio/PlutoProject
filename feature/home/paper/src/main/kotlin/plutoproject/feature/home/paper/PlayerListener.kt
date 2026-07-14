package plutoproject.feature.home.paper

import plutoproject.feature.home.paper.homeManager

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.kernel.api.koinInject
import plutoproject.feature.home.api.paper.HomeManager

@Suppress("UNUSED", "UnusedReceiverParameter")
object PlayerListener : Listener {
    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        // 加载所有家
        homeManager.list(player)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        homeManager.unloadAll(player)
    }
}
