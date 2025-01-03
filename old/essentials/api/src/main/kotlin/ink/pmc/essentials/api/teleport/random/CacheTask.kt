package ink.pmc.essentials.api.teleport.random

import ink.pmc.essentials.api.teleport.TaskState
import org.bukkit.Chunk
import org.bukkit.World
import java.util.*

interface CacheTask {

    val id: UUID
    val world: World
    val options: RandomTeleportOptions
    val attempts: Int
    val cached: Collection<Chunk>
    val state: TaskState
    val isPending: Boolean
    val isTicking: Boolean
    val isFinished: Boolean

    suspend fun tick(): RandomTeleportCache?

    fun cancel()

}