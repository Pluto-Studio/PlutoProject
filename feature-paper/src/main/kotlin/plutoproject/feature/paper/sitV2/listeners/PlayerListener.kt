package plutoproject.feature.paper.sitV2.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDismountEvent
import plutoproject.feature.paper.api.sitV2.Sit

object PlayerListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityDismountEvent.e() {
        if (entity !is Player) return
        val player = entity as Player
        if (!Sit.getState(player).isSitting) return

        isCancelled = true
        Sit.standUp(player)
    }
}
