package plutoproject.feature.sit.paper.player.listeners

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
import plutoproject.feature.sit.api.paper.player.PlayerSit
import plutoproject.feature.sit.api.paper.player.PlayerStackDestroyCause
import plutoproject.feature.sit.api.paper.player.PlayerStackJoinCause
import plutoproject.feature.sit.api.paper.player.PlayerStackQuitCause
import plutoproject.feature.sit.paper.PLAYER_SIT_FAILED_CARRIER_FEATURE_DISABLED
import plutoproject.feature.sit.paper.SIT_FAILED_SOUND
import plutoproject.feature.sit.paper.player.InternalOperationMarker
import plutoproject.kernel.api.koinInject

object PlayerSitPlayerListener : Listener {
    private val playerSit by koinInject<PlayerSit>()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    suspend fun PlayerInteractEntityEvent.e() {
        if (rightClicked !is Player) return
        if (player.isSneaking) return
        if (hand != EquipmentSlot.HAND) return
        if (!player.inventory.itemInMainHand.type.isAir) return
        if (!player.hasPermission("plutoproject.sit.interact.player_right_click")) return
        if (!playerSit.isFeatureEnabled(player)) return
        if (playerSit.isPassenger(player)) return

        player.swingMainHand()
        val target = rightClicked as Player

        if (!target.hasPermission("plutoproject.sit.player_sit.as_seat")) {
            return
        }

        if (!playerSit.isFeatureEnabled(target)) {
            player.showTitle(PLAYER_SIT_FAILED_CARRIER_FEATURE_DISABLED)
            player.playSound(SIT_FAILED_SOUND)
            return
        }

        isCancelled = true

        if (playerSit.isCarrier(player)) {
            val sitStack = playerSit.getStack(player)!!
            sitStack.addPlayerAtBottom(target, cause = PlayerStackJoinCause.RIGHT_CLICK_ON_PLAYER)
        } else {
            val sitStack = playerSit.getStack(target) ?: playerSit.createStack(target) ?: return
            if (!sitStack.addPlayerOnTop(player, cause = PlayerStackJoinCause.RIGHT_CLICK_ON_PLAYER).isSucceed) {
                sitStack.destroy(cause = PlayerStackDestroyCause.RIGHT_CLICK_SIT_CANCELLED)
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerToggleSneakEvent.e() {
        if (!isSneaking) return

        val stack = playerSit.getStack(player) ?: return
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

        val stack = playerSit.getStack(player) ?: return
        isCancelled = true
        stack.removePlayer(player, PlayerStackQuitCause.INITIATIVE)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerDeathEvent.e() {
        val stack = playerSit.getStack(player) ?: return
        stack.removePlayer(player, cause = PlayerStackQuitCause.DEATH)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        val stack = playerSit.getStack(player) ?: return
        stack.removePlayer(player, cause = PlayerStackQuitCause.QUIT)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }
}
