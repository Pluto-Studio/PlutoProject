package plutoproject.feature.elevator.paper

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.launch
import org.bukkit.event.player.PlayerToggleSneakEvent
import plutoproject.feature.elevator.api.paper.ElevatorManager
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

suspend fun handlePlayerJumpFloorUp(event: PlayerJumpEvent, manager: ElevatorManager) {
    val player = event.player
    val context = currentModuleContext() as PaperModuleContext
    context.coroutineScope.launch(context.plugin.minecraftDispatcher) {
        val chain = manager.getChainAt(event.from) ?: return@launch
        chain.up(player)
    }
}

suspend fun handlePlayerSneakFloorDown(event: PlayerToggleSneakEvent, manager: ElevatorManager) {
    if (!event.isSneaking) return
    val player = event.player
    val context = currentModuleContext() as PaperModuleContext
    context.coroutineScope.launch(context.plugin.minecraftDispatcher) {
        val chain = manager.getChainAt(player.location) ?: return@launch
        chain.down(player)
    }
}
