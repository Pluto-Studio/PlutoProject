package plutoproject.feature.elevator.paper.listeners

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import plutoproject.feature.elevator.api.paper.ElevatorManager
import plutoproject.feature.elevator.paper.handlePlayerJumpFloorUp
import plutoproject.feature.elevator.paper.handlePlayerSneakFloorDown
import plutoproject.kernel.api.koinInject

object ElevatorListener : Listener {
    private val manager by koinInject<ElevatorManager>()

    @EventHandler
    suspend fun PlayerJumpEvent.e() {
        handlePlayerJumpFloorUp(this, manager)
    }

    @EventHandler
    suspend fun PlayerToggleSneakEvent.e() {
        handlePlayerSneakFloorDown(this, manager)
    }
}
