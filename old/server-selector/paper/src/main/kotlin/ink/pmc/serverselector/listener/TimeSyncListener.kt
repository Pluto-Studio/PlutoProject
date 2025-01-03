package ink.pmc.serverselector.listener

import ink.pmc.serverselector.startTimeSync
import ink.pmc.serverselector.stopTimeSync
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Suppress("UNUSED")
object TimeSyncListener : Listener {
    @EventHandler
    fun PlayerJoinEvent.e() {
        player.startTimeSync()
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        player.stopTimeSync()
    }
}