package plutoproject.feature.paper.sit.block.listeners

import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause
import plutoproject.feature.paper.sit.block.InternalBlockSit

object BlockSitEntityListener : Listener, KoinComponent {
    private val internalBlockSit by inject<InternalBlockSit>()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityDeathEvent.e() {
        if (entity !is ArmorStand) return
        val armorStand = entity as ArmorStand
        if (!internalBlockSit.isSeatEntityInUse(armorStand)) return
        isCancelled = true
    }

    @EventHandler
    fun EntityRemoveEvent.e() {
        if (entity !is ArmorStand) return
        val armorStand = entity as ArmorStand
        if (!internalBlockSit.isSeatEntityInUse(armorStand)) return
        val owner = internalBlockSit.getSeatEntityOwner(armorStand) ?: return
        internalBlockSit.standUp(owner, StandUpFromBlockCause.SEAT_ENTITY_REMOVE)
    }
}
