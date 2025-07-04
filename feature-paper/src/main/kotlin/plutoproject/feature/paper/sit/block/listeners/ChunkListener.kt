package plutoproject.feature.paper.sit.block.listeners

import org.bukkit.Chunk
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.sit.block.InternalBlockSit

object ChunkListener : Listener, KoinComponent {
    private val internalBlockSit by inject<InternalBlockSit>()

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
            .filter { !internalBlockSit.isSeatEntityInUse(it) }
            .forEach { it.remove() }
    }
}
