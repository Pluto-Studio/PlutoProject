package plutoproject.feature.no_join_quit_message.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Suppress("UNUSED")
object NoJoinQuitMessageListener : Listener {
    @EventHandler
    fun PlayerJoinEvent.e() {
        joinMessage(null)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        quitMessage(null)
    }
}
