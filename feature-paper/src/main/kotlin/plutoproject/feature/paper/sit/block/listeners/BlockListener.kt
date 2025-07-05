package plutoproject.feature.paper.sit.block.listeners

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
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.SeatBlockBreakCause
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause
import plutoproject.feature.paper.api.sit.block.events.SeatBreakEvent

@Suppress("UnusedReceiverParameter")
object BlockListener : Listener {
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
            val sitter = BlockSit.getSitter(it) ?: return@forEach
            val options = BlockSit.getOptions(sitter)!!

            if (callSeatBlockBreakEvent(it, SeatBlockBreakCause.EXPLODE, sitter, options).isCancelled) {
                affected.remove(it)
                return@forEach
            }

            BlockSit.standUp(sitter, StandUpFromBlockCause.SEAT_BREAK)
        }
    }

    private fun Cancellable.handleSeatBlockBreak(block: Block, cause: SeatBlockBreakCause) {
        val sitter = BlockSit.getSitter(block) ?: return
        val options = BlockSit.getOptions(sitter)!!

        if (callSeatBlockBreakEvent(block, cause, sitter, options).isCancelled) {
            isCancelled = true
            return
        }

        BlockSit.standUp(sitter, StandUpFromBlockCause.SEAT_BREAK)
    }
}
