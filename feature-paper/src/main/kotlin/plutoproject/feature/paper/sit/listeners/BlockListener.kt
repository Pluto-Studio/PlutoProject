package plutoproject.feature.paper.sit.listeners

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Piston
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import plutoproject.feature.paper.api.sit.Sit

@Suppress("UnusedReceiverParameter")
object BlockListener : Listener {
    private val extendedPistons = mutableSetOf<Block>()

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
        extendedPistons += block
        blocks.forEach { handleSeatBlockBreak(it) }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun BlockPistonRetractEvent.e() {
        handleSeatBlockBreak(block)
        extendedPistons -= block
        blocks.forEach { handleSeatBlockBreak(it) }
    }

    @EventHandler
    fun ServerTickEndEvent.e() {
        if (extendedPistons.isEmpty()) return // 防止创建不必要的 ArrayList 对象
        val retracted = extendedPistons.filter {
            it.type != Material.MOVING_PISTON && !(it.blockData as Piston).isExtended
        }
        if (retracted.isEmpty()) return
        extendedPistons.removeAll(retracted.toSet())
        retracted.forEach { handleSeatBlockBreak(it) }
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
        val sitter = Sit.getSitterOn(block) ?: return
        Sit.standUp(sitter)
    }
}
