package plutoproject.feature.paper.sit.block.listeners

import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import plutoproject.feature.paper.api.sit.block.BlockSitFinalResult.*
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.SitOnBlockCause
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause
import plutoproject.feature.paper.sit.SIT_FAILED_SOUND
import plutoproject.feature.paper.sit.SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE
import plutoproject.feature.paper.sit.SIT_FAILED_TARGET_OCCUPIED_TITLE

object PlayerListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityDismountEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        if (!BlockSit.isSitting(player)) return

        isCancelled = true
        if (player.isDead) return
        handlePlayerStandUp(this, player, StandUpFromBlockCause.INITIATIVE, true)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        handlePlayerStandUp(this, player, StandUpFromBlockCause.QUIT, false)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        val type = if (player.health - finalDamage <= 0) StandUpFromBlockCause.DEATH else StandUpFromBlockCause.DAMAGE
        handlePlayerStandUp(this, entity as Player, type, false)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerTeleportEvent.e() {
        handlePlayerStandUp(this, player, StandUpFromBlockCause.TELEPORT, true)
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
        val result = BlockSit.sit(player, clickedBlock!!, cause = SitOnBlockCause.RIGHT_CLICK_SEAT)

        val title = when (result) {
            SUCCEED -> null
            ALREADY_SITTING -> null
            SEAT_OCCUPIED -> SIT_FAILED_TARGET_OCCUPIED_TITLE
            INVALID_SEAT -> null
            BLOCKED_BY_BLOCKS -> SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE
            CANCELLED_BY_PLUGIN -> null
        }

        title?.let {
            player.showTitle(it)
            player.playSound(SIT_FAILED_SOUND)
        }
    }

    private fun handlePlayerStandUp(
        event: Event,
        player: Player,
        cause: StandUpFromBlockCause,
        transmitCancelEvent: Boolean
    ) {
        if (!BlockSit.isSitting(player)) return
        if (BlockSit.standUp(player, cause)) return
        if (!transmitCancelEvent || event !is Cancellable) return
        event.isCancelled = true
    }
}
