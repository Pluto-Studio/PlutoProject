package plutoproject.feature.paper.sit.block.listeners

import org.bukkit.Chunk
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import plutoproject.feature.paper.api.sit.block.BlockSit

object BlockSitChunkListener : Listener {
    @EventHandler
    fun ChunkUnloadEvent.e() {
        chunk.removeTemporaryArmorStands()
    }

    @EventHandler
    fun ChunkLoadEvent.e() {
        chunk.removeTemporaryArmorStands()
    }

    private fun Chunk.removeTemporaryArmorStands() {
        entities
            .filter { it is ArmorStand && BlockSit.isTemporarySeatEntity(it) }
            .forEach { it.remove() }
    }
}
