package plutoproject.feature.gallery.adapter.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Suppress("UNUSED")
object GalleryPlayerListener : Listener {
    private val coordinator by koin.inject<GalleryRuntimeCoordinator>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        coordinator.onPlayerJoin(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        coordinator.onPlayerQuit(event.player.uniqueId)
    }
}
