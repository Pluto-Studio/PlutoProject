package plutoproject.feature.gallery.adapter.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED")
object GalleryChunkListener : Listener, KoinComponent {
    private val coordinator by inject<GalleryRuntimeCoordinator>()

    @EventHandler
    suspend fun ChunkLoadEvent.onLoad() {
        coordinator.onChunkLoad(chunk)
    }

    @EventHandler
    fun onUnload(event: ChunkUnloadEvent) {
        coordinator.onChunkUnload(event.chunk)
    }
}
