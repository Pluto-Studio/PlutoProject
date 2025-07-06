package plutoproject.feature.paper.sit.player.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import plutoproject.feature.paper.api.sit.player.PlayerSit

object PlayerListener : Listener {
    @EventHandler
    fun PlayerInteractEntityEvent.e() {
        if (rightClicked is Player) return
        if (hand != EquipmentSlot.HAND) return
        if (!player.inventory.itemInMainHand.type.isAir) return
        if (PlayerSit.getStack(player) != null) return

        val target = rightClicked as Player
        val sitStack = PlayerSit.createStack(target)
    }
}
