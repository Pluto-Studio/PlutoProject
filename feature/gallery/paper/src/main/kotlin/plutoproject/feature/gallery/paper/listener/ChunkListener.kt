package plutoproject.feature.gallery.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import plutoproject.feature.gallery.common.ChunkCallbacks
import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.kernel.api.koinGet

@Suppress("UNUSED")
object ChunkListener : Listener {
    private val callbacks = koinGet<ChunkCallbacks>()

    @EventHandler
    suspend fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk
        callbacks.chunkLoad(event.world.name, ChunkKey(chunk.x, chunk.z))
    }

    @EventHandler
    suspend fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk
        callbacks.chunkUnload(event.world.name, ChunkKey(chunk.x, chunk.z))
    }
}
