package ink.pmc.misc

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import ink.pmc.misc.api.elevator.ElevatorManager
import ink.pmc.framework.concurrent.async
import ink.pmc.framework.platform.isFolia
import org.bukkit.event.player.PlayerToggleSneakEvent

suspend fun handlePlayerJumpFloorUp(event: PlayerJumpEvent) {
    val player = event.player

    /*
    * TODO: 兼容 Folia
    * 暂时没有找到在 Folia 上完美工作的办法。
    * 如果直接使用调度器，会有延迟。
    * 如果识别到 Folia 使用 runBlocking，会完全阻塞当前区域线程。
    * */
    if (isFolia) {
        return
    }

    async {
        val chain = ElevatorManager.getChainAt(event.from) ?: return@async
        chain.up(player)
    }
}

suspend fun handlePlayerSneakFloorDown(event: PlayerToggleSneakEvent) {
    if (!event.isSneaking) {
        return
    }

    if (isFolia) {
        return
    }

    val player = event.player

    async {
        val chain = ElevatorManager.getChainAt(player.location) ?: return@async
        chain.down(player)
    }
}