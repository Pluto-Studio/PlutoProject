package plutoproject.feature.creeperfirework.paper

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent

@Suppress("UNUSED")
object ExplosionListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun EntityExplodeEvent.e() {
        if (entity.type != EntityType.CREEPER) return
        entity.location.launchFirework()
    }

    @EventHandler
    fun EntityDamageByEntityEvent.e() {
        if (damager.type != EntityType.FIREWORK_ROCKET) return
        if (!damager.persistentDataContainer.has(fireworkKey)) return
        isCancelled = true
    }
}
