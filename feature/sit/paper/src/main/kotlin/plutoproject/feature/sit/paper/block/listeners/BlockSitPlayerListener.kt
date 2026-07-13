package plutoproject.feature.sit.paper.block.listeners

import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import plutoproject.feature.sit.api.paper.block.BlockSit
import plutoproject.feature.sit.api.paper.block.BlockSitFinalResult.*
import plutoproject.feature.sit.api.paper.block.SitOnBlockCause
import plutoproject.feature.sit.api.paper.block.StandUpFromBlockCause
import plutoproject.feature.sit.paper.BLOCK_SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE
import plutoproject.feature.sit.paper.BLOCK_SIT_FAILED_TARGET_OCCUPIED_TITLE
import plutoproject.feature.sit.paper.SIT_FAILED_SOUND
import plutoproject.kernel.api.koinInject

object BlockSitPlayerListener : Listener {
    private val blockSit by koinInject<BlockSit>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityDismountEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        if (!blockSit.isSitting(player)) return

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
        if (player.isSneaking) return
        if (hand != EquipmentSlot.HAND) return
        if (!action.isRightClick) return
        if (!player.inventory.itemInMainHand.type.isAir) return
        if (clickedBlock == null) return
        if (!player.hasPermission("plutoproject.sit.interact.block_right_click")) return
        if (blockSit.isSitting(player)) return

        val strategy = blockSit.getStrategy(clickedBlock!!) ?: return
        if (!strategy.shouldSitOnRightClick(player, clickedBlock!!)) return

        isCancelled = true
        player.swingMainHand()
        val result = blockSit.sit(player, clickedBlock!!, cause = SitOnBlockCause.RIGHT_CLICK_SEAT)

        val title = when (result) {
            SUCCEED -> null
            ALREADY_SITTING -> null
            SEAT_OCCUPIED -> BLOCK_SIT_FAILED_TARGET_OCCUPIED_TITLE
            INVALID_SEAT -> null
            BLOCKED_BY_BLOCKS -> BLOCK_SIT_FAILED_TARGET_BLOCKED_BY_BLOCKS_TITLE
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
        if (!blockSit.isSitting(player)) return
        if (blockSit.standUp(player, cause)) return
        if (!transmitCancelEvent || event !is Cancellable) return
        event.isCancelled = true
    }
}
