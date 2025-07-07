package plutoproject.feature.paper.sit.player.listeners

import org.bukkit.entity.AreaEffectCloud
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause
import plutoproject.feature.paper.api.sit.player.PlayerStackQuitCause
import plutoproject.feature.paper.sit.player.InternalOperationMarker
import plutoproject.feature.paper.sit.player.InternalPlayerSit

object PlayerSitEntityListener : Listener, KoinComponent {
    private val internalSit by inject<InternalPlayerSit>()

    @EventHandler
    fun EntityRemoveEvent.e() {
        val entity = entity as? AreaEffectCloud ?: return
        val seatEntityOwner = internalSit.getSeatEntityOwner(entity) ?: return
        val stack = PlayerSit.getStack(seatEntityOwner) ?: return

        if (InternalOperationMarker.isInOperation(seatEntityOwner)) {
            return
        }

        stack.removePlayer(seatEntityOwner, PlayerStackQuitCause.SEAT_ENTITY_REMOVE)

        if (stack.players.size == 1) {
            stack.destroy(PlayerStackDestroyCause.ALL_PASSENGER_LEFT)
        }
    }
}
