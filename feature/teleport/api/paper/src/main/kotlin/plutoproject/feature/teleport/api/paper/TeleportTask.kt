package plutoproject.feature.teleport.api.paper

import org.bukkit.Location
import org.bukkit.entity.Player
import plutoproject.feature.teleport.api.paper.ChunkLocation
import java.util.*

enum class TeleportTaskState {
    PENDING, TICKING, FINISHED
}

interface TeleportTask {
    val id: UUID
    val player: Player
    val destination: Location
    val teleportOptions: TeleportOptions?
    val prompt: Boolean
    val chunkNeedToPrepare: Collection<ChunkLocation>
    val state: TeleportTaskState
    val isPending: Boolean
    val isTicking: Boolean
    val isFinished: Boolean

    suspend fun tick()

    fun cancel()
}
