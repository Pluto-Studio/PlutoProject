package plutoproject.feature.teleport.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.supervisorScope
import org.bukkit.Location
import org.bukkit.entity.Player
import plutoproject.kernel.api.koinInject
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.teleport.api.paper.TeleportOptions
import plutoproject.feature.teleport.api.paper.TeleportTask
import plutoproject.feature.teleport.api.paper.TeleportTaskState
import plutoproject.feature.teleport.api.paper.ChunkLocation
import java.util.*

class TeleportTaskImpl(
    override val id: UUID,
    override val player: Player,
    override val destination: Location,
    override val teleportOptions: TeleportOptions?,
    override val prompt: Boolean,
    override val chunkNeedToPrepare: List<ChunkLocation>,
) : TeleportTask {
    private var scope: CoroutineScope? = null

    override var state: TeleportTaskState = TeleportTaskState.PENDING
    override val isPending: Boolean
        get() = state == TeleportTaskState.PENDING
    override val isTicking: Boolean
        get() = state == TeleportTaskState.TICKING
    override val isFinished: Boolean
        get() = state == TeleportTaskState.FINISHED

    override suspend fun tick() {
        if (isTicking || isFinished) {
            return
        }

        state = TeleportTaskState.TICKING
        supervisorScope {
            scope = this
            teleportManager.prepareChunk(chunkNeedToPrepare, destination.world)
            teleportManager.fireTeleport(player, destination, teleportOptions, prompt)
        }
        state = TeleportTaskState.FINISHED
    }

    override fun cancel() {
        if (isFinished || scope == null) {
            return
        }
        scope?.cancel()
        player.clearTitle()
    }
}
