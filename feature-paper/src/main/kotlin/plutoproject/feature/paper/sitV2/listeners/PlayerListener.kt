package plutoproject.feature.paper.sitV2.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import plutoproject.feature.paper.api.sitV2.Sit
import plutoproject.feature.paper.api.sitV2.SitFinalResult.*
import plutoproject.feature.paper.sitV2.SIT_FAILED_BLOCKED_BY_BLOCKS_TITLE
import plutoproject.feature.paper.sitV2.SIT_FAILED_SOUND
import plutoproject.feature.paper.sitV2.SIT_FAILED_TARGET_OCCUPIED_TITLE

object PlayerListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityDismountEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        if (!Sit.getState(player).isSitting) return

        isCancelled = true
        Sit.standUp(player)
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
        if (Sit.getState(player).isSitting) return

        val strategy = Sit.getStrategy(clickedBlock!!) ?: return
        if (!strategy.shouldSitOnRightClick(player, clickedBlock!!)) return

        isCancelled = true
        player.swingMainHand()
        val result = Sit.sitOnBlock(player, clickedBlock!!)

        val title = when (result) {
            SUCCEED -> null
            FAILED_ALREADY_SITTING -> null
            FAILED_TARGET_OCCUPIED -> SIT_FAILED_TARGET_OCCUPIED_TITLE
            FAILED_INVALID_TARGET -> null
            FAILED_BLOCKED_BY_BLOCKS -> SIT_FAILED_BLOCKED_BY_BLOCKS_TITLE
            FAILED_CANCELLED_BY_PLUGIN -> null
        }

        title?.let {
            player.showTitle(it)
            player.playSound(SIT_FAILED_SOUND)
        }
    }

    private fun handlePlayerStandUp(player: Player) {
        if (!Sit.getState(player).isSitting) return
        Sit.standUp(player)
    }
}
