package plutoproject.feature.paper.whitelist_v2

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.whitelist_v2.WhitelistImpl
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
object VisitorListener : Listener, KoinComponent {
    private val whitelist by lazy { get<Whitelist>() as WhitelistImpl }
    private val pendingVisitors = ConcurrentHashMap<UUID, Job>()

    fun onVisitorIncoming(uniqueId: UUID, username: String) {
        whitelist.addKnownVisitor(uniqueId)

        val timeoutJob = PluginScope.launch {
            delay(30.seconds)
            if (whitelist.isKnownVisitor(uniqueId)) {
                whitelist.removeKnownVisitor(uniqueId)
            }
            pendingVisitors.remove(uniqueId)
            featureLogger.warning("待处理的访客 $username ($uniqueId) 超时未连接")
        }

        pendingVisitors[uniqueId] = timeoutJob
        featureLogger.info("收到访客进入通知: $username ($uniqueId)")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            pendingVisitors.remove(player.uniqueId)?.cancel()
            player.gameMode = GameMode.SPECTATOR
            hideVisitorFromAllPlayers(player)
            featureLogger.info("访客进入: ${player.name} (${player.uniqueId})")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            whitelist.removeKnownVisitor(player.uniqueId)
            pendingVisitors.remove(player.uniqueId)?.cancel()
            featureLogger.info("访客退出: ${player.name} (${player.uniqueId})")
        }
    }

    private fun hideVisitorFromAllPlayers(visitor: Player) {
        server.onlinePlayers.forEach { otherPlayer ->
            if (otherPlayer.uniqueId != visitor.uniqueId) {
                otherPlayer.hidePlayer(plugin, visitor)
                if (whitelist.isKnownVisitor(otherPlayer.uniqueId)) {
                    visitor.hidePlayer(plugin, otherPlayer)
                }
            }
        }
    }

    @EventHandler
    fun onNonVisitorPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!whitelist.isKnownVisitor(player.uniqueId)) {
            server.onlinePlayers.forEach { onlinePlayer ->
                if (whitelist.isKnownVisitor(onlinePlayer.uniqueId)) {
                    player.hidePlayer(plugin, onlinePlayer)
                }
            }
        }
    }
}
