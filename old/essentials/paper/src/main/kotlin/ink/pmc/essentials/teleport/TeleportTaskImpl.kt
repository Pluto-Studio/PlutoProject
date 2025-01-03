package ink.pmc.essentials.teleport

import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.api.teleport.TeleportOptions
import ink.pmc.essentials.api.teleport.TeleportTask
import ink.pmc.essentials.api.teleport.TeleportTaskState
import ink.pmc.framework.world.ValueVec2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.supervisorScope
import org.bukkit.Location
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class TeleportTaskImpl(
    override val id: UUID,
    override val player: Player,
    override val destination: Location,
    override val teleportOptions: TeleportOptions?,
    override val prompt: Boolean,
    override val chunkNeedToPrepare: List<ValueVec2>,
) : TeleportTask, KoinComponent {
    private val manager by inject<TeleportManager>()
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
            manager.prepareChunk(chunkNeedToPrepare, destination.world)
            manager.fireTeleport(player, destination, teleportOptions, prompt)
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