package plutoproject.framework.paper.util.world.chunk

import net.minecraft.server.level.TicketType
import org.bukkit.Chunk
import org.bukkit.craftbukkit.CraftWorld

fun <T> Chunk.addTicket(type: TicketType<T>, x: Int, z: Int, level: Int, identifier: T) {
    val handle = (this.world as CraftWorld).handle.chunkSource
    val distanceManager = handle.chunkMap.distanceManager
    val holder = distanceManager.chunkHolderManager
    holder.addTicketAtLevel(type, x, z, level, identifier)
}

fun <T> Chunk.removeTicket(type: TicketType<T>, x: Int, z: Int, level: Int, identifier: T) {
    val handle = (this.world as CraftWorld).handle.chunkSource
    val distanceManager = handle.chunkMap.distanceManager
    val holder = distanceManager.chunkHolderManager
    holder.removeTicketAtLevel(type, x, z, level, identifier)
}
