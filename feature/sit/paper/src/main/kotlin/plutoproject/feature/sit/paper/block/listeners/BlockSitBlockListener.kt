package plutoproject.feature.sit.paper.block.listeners

import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import plutoproject.feature.sit.api.paper.SitOptions
import plutoproject.feature.sit.api.paper.block.BlockSit
import plutoproject.feature.sit.api.paper.block.SeatBlockBreakCause
import plutoproject.feature.sit.api.paper.block.StandUpFromBlockCause
import plutoproject.feature.sit.api.paper.block.events.SeatBreakEvent
import plutoproject.kernel.api.koinInject

@Suppress("UnusedReceiverParameter")
object BlockSitBlockListener : Listener {
    private val blockSit by koinInject<BlockSit>()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockBreakEvent.e() {
        handleSeatBlockBreak(block, SeatBlockBreakCause.PLAYER)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockExplodeEvent.e() {
        handleExplosionSeatBlockBreak(blockList())
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockBurnEvent.e() {
        handleSeatBlockBreak(block, SeatBlockBreakCause.BURN)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockFadeEvent.e() {
        handleSeatBlockBreak(block, SeatBlockBreakCause.FADE)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityExplodeEvent.e() {
        handleExplosionSeatBlockBreak(blockList())
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun LeavesDecayEvent.e() {
        handleSeatBlockBreak(block, SeatBlockBreakCause.DECAY)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun EntityChangeBlockEvent.e() {
        if (entity.type != EntityType.FALLING_BLOCK) return
        handleSeatBlockBreak(block, SeatBlockBreakCause.FALL)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun TNTPrimeEvent.e() {
        handleSeatBlockBreak(block, SeatBlockBreakCause.TNT_PRIME)
    }

    private fun callSeatBlockBreakEvent(
        seat: Block,
        cause: SeatBlockBreakCause,
        sitter: Player,
        options: SitOptions
    ): SeatBreakEvent {
        return SeatBreakEvent(seat, cause, sitter, options).apply { callEvent() }
    }

    private fun handleExplosionSeatBlockBreak(affected: MutableList<Block>) {
        affected.toList().forEach {
            val sitter = blockSit.getSitter(it) ?: return@forEach
            val options = blockSit.getOptions(sitter)!!

            if (callSeatBlockBreakEvent(it, SeatBlockBreakCause.EXPLODE, sitter, options).isCancelled) {
                affected.remove(it)
                return@forEach
            }

            blockSit.standUp(sitter, StandUpFromBlockCause.SEAT_BREAK)
        }
    }

    private fun Cancellable.handleSeatBlockBreak(block: Block, cause: SeatBlockBreakCause) {
        val sitter = blockSit.getSitter(block) ?: return
        val options = blockSit.getOptions(sitter)!!

        if (callSeatBlockBreakEvent(block, cause, sitter, options).isCancelled) {
            isCancelled = true
            return
        }

        blockSit.standUp(sitter, StandUpFromBlockCause.SEAT_BREAK)
    }
}
