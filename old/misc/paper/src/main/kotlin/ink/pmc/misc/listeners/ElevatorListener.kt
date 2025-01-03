package ink.pmc.misc.listeners

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import ink.pmc.misc.handlePlayerJumpFloorUp
import ink.pmc.misc.handlePlayerSneakFloorDown
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent

@Suppress("UNUSED")
object ElevatorListener : Listener {

    @EventHandler
    suspend fun PlayerJumpEvent.e() {
        handlePlayerJumpFloorUp(this)
    }

    @EventHandler
    suspend fun PlayerToggleSneakEvent.e() {
        handlePlayerSneakFloorDown(this)
    }

}