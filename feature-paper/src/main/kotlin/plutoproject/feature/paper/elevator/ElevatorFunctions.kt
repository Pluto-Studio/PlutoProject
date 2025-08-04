package plutoproject.feature.paper.elevator

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import kotlinx.coroutines.launch
import org.bukkit.event.player.PlayerToggleSneakEvent
import plutoproject.feature.paper.api.elevator.ElevatorManager
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.coroutine.coroutineContext

suspend fun handlePlayerJumpFloorUp(event: PlayerJumpEvent) {
    val player = event.player
    PluginScope.launch(player.coroutineContext) {
        val chain = ElevatorManager.getChainAt(event.from) ?: return@launch
        chain.up(player)
    }
}

suspend fun handlePlayerSneakFloorDown(event: PlayerToggleSneakEvent) {
    if (!event.isSneaking) return
    val player = event.player
    PluginScope.launch(player.coroutineContext) {
        val chain = ElevatorManager.getChainAt(player.location) ?: return@launch
        chain.down(player)
    }
}
