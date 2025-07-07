package plutoproject.feature.paper.sit.player.listeners

import org.bukkit.Chunk
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.sit.player.InternalPlayerSit

object PlayerSitChunkListener : Listener, KoinComponent {
    private val internalSit by inject<InternalPlayerSit>()

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
            .filter { !internalSit.isSeatEntityInUse(it) }
            .forEach { it.remove() }
    }
}
