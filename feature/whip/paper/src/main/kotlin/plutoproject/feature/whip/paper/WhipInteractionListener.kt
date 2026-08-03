package plutoproject.feature.whip.paper

import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PlayerLeashEntityEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerUnleashEntityEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

internal class WhipInteractionListener(
    private val sessions: WhipSessionManager,
) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (
            event.useItemInHand() == Event.Result.DENY ||
            event.action !in RIGHT_CLICK_ACTIONS
        ) {
            return
        }
        val item = event.item ?: return
        if (inspectWhip(item) !is WhipIdentity.Valid) {
            return
        }
        val hand = event.hand ?: return
        if (!hand.isHand) {
            return
        }

        // A custom whip must never reach the vanilla block/lead path. Ordinary leads never
        // enter this branch because identity is checked before changing either result.
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.ALLOW)
        beginUse(event.player, hand)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.isCancelled) {
            return
        }
        val hand = event.hand
        if (!hand.isHand || !isWhipInHand(event.player, hand)) {
            return
        }

        event.isCancelled = true
        beginUse(event.player, hand)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onPlayerLeash(event: PlayerLeashEntityEvent) {
        if (isWhipInHand(event.player, event.hand)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onPlayerUnleash(event: PlayerUnleashEntityEvent) {
        if (isWhipInHand(event.player, event.hand)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onHeldItemChanged(event: PlayerItemHeldEvent) {
        sessions.stopIfHand(event.player, EquipmentSlot.HAND)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        sessions.stopIfDropped(event.player, event.itemDrop.itemStack)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        sessions.stop(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        sessions.stop(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        sessions.stop(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onDeath(event: PlayerDeathEvent) {
        sessions.stop(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onQuit(event: PlayerQuitEvent) {
        sessions.stop(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onKick(event: PlayerKickEvent) {
        sessions.stop(event.player)
    }

    private fun beginUse(player: Player, hand: EquipmentSlot) {
        val heldItem = player.inventory.getItem(hand)
        if (inspectWhip(heldItem) !is WhipIdentity.Valid) {
            return
        }
        heldItem.configureWhipUse()
        player.inventory.setItem(hand, heldItem)
        player.startUsingItem(hand)
        sessions.start(player, hand, heldItem)
    }

    private fun isWhipInHand(player: Player, hand: EquipmentSlot?): Boolean {
        if (hand == null || !hand.isHand) {
            return false
        }
        return inspectWhip(player.inventory.getItem(hand)) is WhipIdentity.Valid
    }

    private companion object {
        val RIGHT_CLICK_ACTIONS = setOf(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK)
    }
}
