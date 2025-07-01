package plutoproject.feature.paper.sitV2.listeners

import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import plutoproject.feature.paper.api.sitV2.Sit

@Suppress("UnusedReceiverParameter")
object BlockListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockBreakEvent.e() {
        handleSeatBlockBreak(block)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockExplodeEvent.e() {
        blockList().forEach { handleSeatBlockBreak(it) }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockBurnEvent.e() {
        handleSeatBlockBreak(block)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockFadeEvent.e() {
        handleSeatBlockBreak(block)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityExplodeEvent.e() {
        blockList().forEach { handleSeatBlockBreak(it) }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockPistonExtendEvent.e() {
        handleSeatBlockBreak(block)
        blocks.forEach { handleSeatBlockBreak(it) }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockPistonRetractEvent.e() {
        handleSeatBlockBreak(block)
        blocks.forEach { handleSeatBlockBreak(it) }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun LeavesDecayEvent.e() {
        handleSeatBlockBreak(block)
    }

    private fun handleSeatBlockBreak(block: Block) {
        val sitter = Sit.getSitterOn(block) ?: return
        Sit.standUp(sitter)
    }
}
