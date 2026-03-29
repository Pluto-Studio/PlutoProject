package plutoproject.feature.gallery.adapter.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

@Suppress("UNUSED")
object GalleryChunkListener : Listener {
    private val coordinator by koin.inject<GalleryRuntimeCoordinator>()

    @EventHandler
    suspend fun ChunkLoadEvent.onLoad() {
        coordinator.onChunkLoad(chunk)
    }

    @EventHandler
    fun onUnload(event: ChunkUnloadEvent) {
        coordinator.onChunkUnload(event.chunk)
    }
}
