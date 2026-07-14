package plutoproject.feature.no_creeper_block_breaks.paper

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent

object ExplosionListener : Listener {
    @EventHandler
    fun EntityExplodeEvent.e() {
        if (entity.type != EntityType.CREEPER) return
        blockList().clear()
    }
}
