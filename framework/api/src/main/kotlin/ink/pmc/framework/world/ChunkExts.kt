package ink.pmc.framework.world

import ink.pmc.framework.concurrent.submitSync
import ink.pmc.framework.platform.isFoliaOrAsync
import net.minecraft.server.level.TicketType
import org.bukkit.Chunk
import org.bukkit.craftbukkit.CraftWorld

@Suppress("UNUSED")
fun Chunk.ensureThreadSafe(block: Chunk.() -> Unit) {
    if (isFoliaOrAsync) {
        this.submitSync {
            block()
        }
        return
    }
    block()
}

fun <T> Chunk.addTicket(type: TicketType<T>, x: Int, z: Int, level: Int, identifier: T) {
    val handle = (this.world as CraftWorld).handle.chunkSource
    val distanceManager = handle.chunkMap.distanceManager
    val holder = distanceManager.`moonrise$getChunkHolderManager`()
    holder.addTicketAtLevel(type, x, z, level, identifier)
}

fun <T> Chunk.removeTicket(type: TicketType<T>, x: Int, z: Int, level: Int, identifier: T) {
    val handle = (this.world as CraftWorld).handle.chunkSource
    val distanceManager = handle.chunkMap.distanceManager
    val holder = distanceManager.`moonrise$getChunkHolderManager`()
    holder.removeTicketAtLevel(type, x, z, level, identifier)
}