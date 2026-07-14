package plutoproject.feature.farmprotection.paper

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

@Suppress("UNUSED")
object InteractionListener : Listener {
    @EventHandler
    fun PlayerInteractEvent.e() {
        if (action == Action.PHYSICAL && clickedBlock?.type == Material.FARMLAND) {
            isCancelled = true
        }
    }
}
