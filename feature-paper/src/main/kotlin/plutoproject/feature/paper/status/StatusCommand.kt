package plutoproject.feature.paper.status

import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.raw
import ink.pmc.advkt.send
import kotlinx.coroutines.withContext
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.CraftWorld
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.paper.util.coroutine.coroutineContext

@Suppress("UNUSED")
object StatusCommand {
    @Command("status|tps|mspt")
    @Permission("plutoproject.status.command.status")
    suspend fun CommandSender.status() {
        val statusMessage = withContext(server.coroutineContext) { getStatusMessage() }
        send {
            raw(INDICATOR)
            raw(statusMessage)
            newline()
            newline()
            raw(INDICATOR)
            raw(getPromptMessage())
        }
    }

    @Command("status chunk <world> <chunkX> <chunkZ>")
    @Permission("plutoproject.status.command.status.chunk")
    suspend fun CommandSender.statusChunk(
        @Argument("world") world: World,
        @Argument("chunkX") chunkX: Int,
        @Argument("chunkZ") chunkZ: Int,
    ) {
        val (level, status) = withContext(server.coroutineContext) {
            val handle = (world as CraftWorld).handle.chunkSource
            val distanceManager = handle.chunkMap.distanceManager
            val holderManager = distanceManager.`moonrise$getChunkHolderManager`()
            val holder = holderManager.getChunkHolder(chunkX, chunkZ) ?: return@withContext null to null
            holder.ticketLevel to holder.chunkStatus
        }
        sendMessage(
            COMMAND_STATUS_CHUNK
                .replace("<world>", world.name)
                .replace("<chunkX>", chunkX)
                .replace("<chunkZ>", chunkZ)
                .replace("<level>", level)
                .replace("<status>", status)
        )
    }
}
