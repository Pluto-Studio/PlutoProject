package plutoproject.feature.paper.sit.block.listeners

import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import plutoproject.feature.paper.api.sit.block.BlockSit

@Suppress("UnusedReceiverParameter")
object BlockSitBlockListener : Listener {
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
    fun LeavesDecayEvent.e() {
        handleSeatBlockBreak(block)
    }

    private val entityFormBlocks = arrayOf(
        EntityType.FALLING_BLOCK,
        EntityType.TNT
    )

    @EventHandler(priority = EventPriority.LOWEST)
    fun EntitySpawnEvent.e() {
        if (!entityFormBlocks.contains(entity.type)) return
        handleSeatBlockBreak(location.block)
    }

    private fun handleSeatBlockBreak(block: Block) {
        val sitter = BlockSit.getSitter(block) ?: return
        BlockSit.standUp(sitter)
    }
}
