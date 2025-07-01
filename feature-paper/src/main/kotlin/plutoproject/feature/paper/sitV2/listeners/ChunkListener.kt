package plutoproject.feature.paper.sitV2.listeners

import org.bukkit.Chunk
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import plutoproject.feature.paper.api.sitV2.Sit

object ChunkListener : Listener {
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
            .filter { it is ArmorStand && Sit.isTemporaryArmorStand(it) }
            .forEach { it.remove() }
    }
}
