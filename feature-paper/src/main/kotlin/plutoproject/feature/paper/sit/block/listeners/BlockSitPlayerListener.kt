package plutoproject.feature.paper.sit.block.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import plutoproject.feature.paper.api.sit.SitFinalResult.*
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.events.PlayerStandUpFromBlockEvent
import plutoproject.feature.paper.api.sit.player.events.PlayerStandUpFromPlayerEvent
import plutoproject.feature.paper.sit.SIT_FAILED_SOUND
import plutoproject.feature.paper.sit.SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE
import plutoproject.feature.paper.sit.SIT_FAILED_TARGET_OCCUPIED_TITLE

object BlockSitPlayerListener : Listener {
    private val standingUp = mutableSetOf<Player>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityDismountEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        if (!BlockSit.isSitting(player)) return
        if (standingUp.contains(player)) {
            standingUp.remove(player)
            return
        }

        isCancelled = true
        BlockSit.standUp(player)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerStandUpFromBlockEvent.e() {
        standingUp.add(player)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerStandUpFromPlayerEvent.e() {
        standingUp.add(player)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        handlePlayerStandUp(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        handlePlayerStandUp(player)
    }

    @EventHandler
    fun PlayerInteractEvent.e() {
        if (hand != EquipmentSlot.HAND) return
        if (!action.isRightClick) return
        if (!player.inventory.itemInMainHand.type.isAir) return
        if (clickedBlock == null) return
        if (BlockSit.isSitting(player)) return

        val strategy = BlockSit.getStrategy(clickedBlock!!) ?: return
        if (!strategy.shouldSitOnRightClick(player, clickedBlock!!)) return

        isCancelled = true
        player.swingMainHand()
        val result = BlockSit.sit(player, clickedBlock!!)

        val title = when (result) {
            SUCCEED -> null
            FAILED_ALREADY_SITTING -> null
            FAILED_TARGET_OCCUPIED -> SIT_FAILED_TARGET_OCCUPIED_TITLE
            FAILED_INVALID_TARGET -> null
            FAILED_TARGET_BLOCKED_BY_BLOCKS -> SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE
            FAILED_CANCELLED_BY_PLUGIN -> null
        }

        title?.let {
            player.showTitle(it)
            player.playSound(SIT_FAILED_SOUND)
        }
    }

    private fun handlePlayerStandUp(player: Player) {
        if (!BlockSit.isSitting(player)) return
        BlockSit.standUp(player)
    }
}
