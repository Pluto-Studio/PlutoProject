package plutoproject.feature.gallery.adapter.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.core.display.job.SendJobRegistry

@Suppress("UNUSED")
object PlayerListener : Listener {
    private val sendJobRegistry = koin.get<SendJobRegistry>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        sendJobRegistry.start(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        sendJobRegistry.stop(event.player.uniqueId)
    }
}
