package plutoproject.feature.paper.sit.player.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause
import plutoproject.feature.paper.api.sit.player.PlayerStackJoinCause
import plutoproject.feature.paper.api.sit.player.PlayerStackQuitCause
import plutoproject.feature.paper.sit.PLAYER_SIT_FAILED_CARRIER_FEATURE_DISABLED
import plutoproject.feature.paper.sit.SIT_FAILED_SOUND
import plutoproject.feature.paper.sit.player.InternalOperationMarker

object PlayerSitPlayerListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    suspend fun PlayerInteractEntityEvent.e() {
        if (rightClicked !is Player) return
        if (hand != EquipmentSlot.HAND) return
        if (!player.inventory.itemInMainHand.type.isAir) return
        if (!PlayerSit.isFeatureEnabled(player)) return
        if (PlayerSit.isPassenger(player)) return

        player.swingMainHand()
        val target = rightClicked as Player

        if (!PlayerSit.isFeatureEnabled(target)) {
            player.showTitle(PLAYER_SIT_FAILED_CARRIER_FEATURE_DISABLED)
            player.playSound(SIT_FAILED_SOUND)
            return
        }

        isCancelled = true

        if (PlayerSit.isCarrier(player)) {
            val sitStack = PlayerSit.getStack(player)!!
            sitStack.addPlayerAtBottom(target, cause = PlayerStackJoinCause.RIGHT_CLICK_ON_PLAYER)
        } else {
            val sitStack = PlayerSit.getStack(target) ?: PlayerSit.createStack(target) ?: return
            if (!sitStack.addPlayerOnTop(player, cause = PlayerStackJoinCause.RIGHT_CLICK_ON_PLAYER).isSucceed) {
                sitStack.destroy(cause = PlayerStackDestroyCause.RIGHT_CLICK_SIT_CANCELLED)
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerToggleSneakEvent.e() {
        if (!isSneaking) return

        val stack = PlayerSit.getStack(player) ?: return
        if (stack.carrier != player) return
        isCancelled = true

        stack.removePlayerAtBottom(PlayerStackQuitCause.INITIATIVE)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityDismountEvent.e() {
        val player = entity as? Player ?: return

        if (InternalOperationMarker.isInOperation(player)) {
            return
        }

        val stack = PlayerSit.getStack(player) ?: return
        isCancelled = true
        stack.removePlayer(player, PlayerStackQuitCause.INITIATIVE)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerDeathEvent.e() {
        val stack = PlayerSit.getStack(player) ?: return
        stack.removePlayer(player, cause = PlayerStackQuitCause.DEATH)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        val stack = PlayerSit.getStack(player) ?: return
        stack.removePlayer(player, cause = PlayerStackQuitCause.QUIT)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }
}
