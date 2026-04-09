package plutoproject.feature.gallery.adapter.paper.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import plutoproject.feature.gallery.adapter.common.chunkLoad
import plutoproject.feature.gallery.adapter.common.chunkUnload
import plutoproject.feature.gallery.core.util.ChunkKey

@Suppress("UNUSED")
object ChunkListener : Listener {
    @EventHandler
    suspend fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk
        chunkLoad(event.world.name, ChunkKey(chunk.x, chunk.z))
    }

    @EventHandler
    suspend fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk
        chunkUnload(event.world.name, ChunkKey(chunk.x, chunk.z))
    }
}
