package plutoproject.feature.gallery.adapter.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED")
object GalleryPlayerListener : Listener, KoinComponent {
    private val coordinator by inject<GalleryRuntimeCoordinator>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        coordinator.onPlayerJoin(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        coordinator.onPlayerQuit(event.player.uniqueId)
    }
}
