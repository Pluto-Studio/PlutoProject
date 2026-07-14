package plutoproject.feature.sit.paper.player.listeners

import org.bukkit.Chunk
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import plutoproject.feature.sit.paper.player.InternalPlayerSit

object PlayerSitChunkListener : Listener {
    private val internalSit by plutoproject.kernel.api.koinInject<InternalPlayerSit>()

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
            .filterIsInstance<AreaEffectCloud>()
            .filter { internalSit.isTemporarySeatEntity(it) && !internalSit.isSeatEntityInUse(it) }
            .forEach { it.remove() }
    }
}
