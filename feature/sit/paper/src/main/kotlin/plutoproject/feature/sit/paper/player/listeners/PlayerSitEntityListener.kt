package plutoproject.feature.sit.paper.player.listeners

import org.bukkit.entity.AreaEffectCloud
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import plutoproject.feature.sit.api.paper.player.PlayerStackDestroyCause
import plutoproject.feature.sit.api.paper.player.PlayerStackQuitCause
import plutoproject.feature.sit.paper.player.InternalOperationMarker
import plutoproject.feature.sit.paper.player.InternalPlayerSit

object PlayerSitEntityListener : Listener {
    private val internalSit by plutoproject.kernel.api.koinInject<InternalPlayerSit>()

    @EventHandler
    fun EntityRemoveEvent.e() {
        val entity = entity as? AreaEffectCloud ?: return
        val seatEntityOwner = internalSit.getSeatEntityOwner(entity) ?: return
        val stack = internalSit.getStack(seatEntityOwner) ?: return

        if (InternalOperationMarker.isInOperation(seatEntityOwner)) {
            return
        }

        stack.removePlayer(seatEntityOwner, PlayerStackQuitCause.SEAT_ENTITY_REMOVE)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }
}
