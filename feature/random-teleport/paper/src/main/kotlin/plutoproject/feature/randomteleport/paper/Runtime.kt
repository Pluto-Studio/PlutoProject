package plutoproject.feature.randomteleport.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import net.minecraft.server.level.TicketType
import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.Player
import plutoproject.feature.randomteleport.api.paper.RandomTeleportManager
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import kotlin.coroutines.CoroutineContext

internal val moduleContext: PaperModuleContext
    get() = currentModuleContext() as PaperModuleContext
internal val moduleScope
    get() = currentModuleContext().coroutineScope
internal val plugin
    get() = moduleContext.plugin
internal val server: Server
    get() = plugin.server
internal val randomTeleportManager: RandomTeleportManager
    get() = koinGet()
internal val teleportManager: TeleportManager
    get() = currentModuleContext().services.getService()
internal val Player.coroutineContext: CoroutineContext
    get() = plugin.minecraftDispatcher
internal fun <T> Chunk.addTicket(type: TicketType<*>, x: Int, z: Int, level: Int, identifier: T) {
    val manager = (world as CraftWorld).handle.chunkSource.chunkMap.distanceManager
        .`moonrise$getChunkHolderManager`()
    manager.addTicketAtLevel(type, x, z, level, identifier)
}

internal fun <T> Chunk.removeTicket(type: TicketType<*>, x: Int, z: Int, level: Int, identifier: T) {
    val manager = (world as CraftWorld).handle.chunkSource.chunkMap.distanceManager
        .`moonrise$getChunkHolderManager`()
    manager.removeTicketAtLevel(type, x, z, level, identifier)
}
