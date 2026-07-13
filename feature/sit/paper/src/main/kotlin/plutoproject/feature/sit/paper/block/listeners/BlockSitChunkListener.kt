package plutoproject.feature.sit.paper.block.listeners

import org.bukkit.Chunk
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import plutoproject.feature.sit.paper.block.InternalBlockSit

object BlockSitChunkListener : Listener {
    private val internalBlockSit by plutoproject.kernel.api.koinInject<InternalBlockSit>()

    @EventHandler
    fun ChunkUnloadEvent.e() {
        chunk.removeTemporarySeatEntities()
    }

    @EventHandler
    fun ChunkLoadEvent.e() {
        chunk.removeTemporarySeatEntities()
    }

    private fun Chunk.removeTemporarySeatEntities() {
        entities
            .filterIsInstance<ArmorStand>()
            .filter { internalBlockSit.isTemporarySeatEntity(it) && !internalBlockSit.isSeatEntityInUse(it) }
            .forEach { it.remove() }
    }
}
