package plutoproject.feature.whitelist.paper

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
import plutoproject.feature.whitelist.api.WhitelistService
import kotlin.time.Duration.Companion.milliseconds
import plutoproject.kernel.api.koinGet

@Suppress("UNUSED")
object VisitorRestrictionListener : Listener {
    private val service = koinGet<WhitelistService>()

    private var visitorSpeedLimitationJob: Job? = null

    fun startVisitorSpeedLimitationJob() {
        visitorSpeedLimitationJob = moduleScope.launch {
            while (isActive) {
                delay(500.milliseconds)
                runVisitorSpeedLimitation()
            }
        }
    }

    private fun runVisitorSpeedLimitation() {
        if (server.onlinePlayers.isEmpty()) {
            return
        }
        server.onlinePlayers
            .filter { service.isKnownVisitor(it.uniqueId) && it.gameMode == GameMode.SPECTATOR }
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
        if (service.isKnownVisitor(player.uniqueId)) {
            player.sendMessage(VISITOR_CHAT_DENIED)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerStartSpectatingEntity(event: PlayerStartSpectatingEntityEvent) {
        val player = event.player
        if (service.isKnownVisitor(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (service.isKnownVisitor(player.uniqueId) && event.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerAdvancementCriterionGrant(event: PlayerAdvancementCriterionGrantEvent) {
        val player = event.player
        if (service.isKnownVisitor(player.uniqueId)) {
            event.isCancelled = true
        }
    }
}
