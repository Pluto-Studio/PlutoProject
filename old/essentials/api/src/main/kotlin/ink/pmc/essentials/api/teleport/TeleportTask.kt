package ink.pmc.essentials.api.teleport

import ink.pmc.framework.world.ValueVec2
import org.bukkit.Location
import org.bukkit.entity.Player
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
    val chunkNeedToPrepare: Collection<ValueVec2>
    val state: TeleportTaskState
    val isPending: Boolean
    val isTicking: Boolean
    val isFinished: Boolean

    suspend fun tick()

    fun cancel()

}