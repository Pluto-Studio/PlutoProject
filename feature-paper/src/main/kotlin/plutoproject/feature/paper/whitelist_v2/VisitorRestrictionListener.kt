package plutoproject.feature.paper.whitelist_v2

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.time.ticks
import plutoproject.framework.paper.util.server

@Suppress("UNUSED")
object VisitorRestrictionListener : Listener {
    private var visitorSpeedLimitationJob: Job? = null

    fun startVisitorSpeedLimitationJob() {
        visitorSpeedLimitationJob = PluginScope.launch {
            while (isActive) {
                delay(10.ticks)
                runVisitorSpeedLimitation()
            }
        }
    }

    private fun runVisitorSpeedLimitation() {
        if (server.onlinePlayers.isEmpty()) {
            return
        }
        server.onlinePlayers
            .filter { Whitelist.isKnownVisitor(it.uniqueId) }
            .forEach {
                // setFlySpeed 只是发送数据包，可以异步
                it.flySpeed = 0.1f
            }
    }

    fun stopVisitorSpeedLimitationJob() {
        visitorSpeedLimitationJob?.cancel()
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        if (Whitelist.isKnownVisitor(player.uniqueId)) {
            player.sendMessage(VISITOR_CHAT_DENIED)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerStartSpectatingEntity(event: PlayerStartSpectatingEntityEvent) {
        val player = event.player
        if (Whitelist.isKnownVisitor(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (Whitelist.isKnownVisitor(player.uniqueId) && event.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerToggleSprint(event: PlayerToggleSprintEvent) {
        val player = event.player
        if (Whitelist.isKnownVisitor(player.uniqueId) && player.gameMode == GameMode.SPECTATOR) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerAdvancementCriterionGrant(event: PlayerAdvancementCriterionGrantEvent) {
        val player = event.player
        if (Whitelist.isKnownVisitor(player.uniqueId)) {
            event.isCancelled = true
        }
    }
}
