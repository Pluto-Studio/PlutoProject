package plutoproject.feature.whitelist_v2.adapter.paper

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.whitelist_v2.adapter.common.KnownVisitors
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
object VisitorListener : Listener, KoinComponent {
    private val whitelist by inject<Whitelist>()
    private val knownVisitors by inject<KnownVisitors>()
    private val pendingVisitors = ConcurrentHashMap<UUID, Job>()

    fun onVisitorIncoming(uniqueId: UUID, username: String) {
        knownVisitors.add(uniqueId)

        val timeoutJob = PluginScope.launch {
            delay(30.seconds)
            if (whitelist.isKnownVisitor(uniqueId)) {
                knownVisitors.remove(uniqueId)
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
    // 现有的 Warp 系统获取 Spawn 是 suspend 的，挂起事件会导致玩家断开后再尝试传送，只能先 runBlocking 了
    fun onPlayerQuit(event: PlayerQuitEvent): Unit = runBlocking {
        val player = event.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            val warpSpawn = if (FeatureManager.isEnabled("warp")) WarpManager.getDefaultSpawn()?.location else null
            val spawn = warpSpawn ?: player.world.spawnLocation
            player.teleport(spawn)
            player.gameMode = GameMode.SURVIVAL
            knownVisitors.remove(player.uniqueId)
            pendingVisitors.remove(player.uniqueId)?.cancel()
            featureLogger.info("访客退出: ${player.name} (${player.uniqueId})")
        }
    }

    private fun hideVisitorFromAllPlayers(visitor: Player) {
        server.onlinePlayers
            .filter { it.uniqueId != visitor.uniqueId }
            .forEach { otherPlayer ->
                if (!otherPlayer.hasPermission(PERMISSION_WHITELIST_SEE_VISITORS)) {
                    otherPlayer.hidePlayer(plugin, visitor)
                }
                // 访客无法看到其他访客
                if (whitelist.isKnownVisitor(otherPlayer.uniqueId)) {
                    visitor.hidePlayer(plugin, otherPlayer)
                }
            }
    }

    @EventHandler
    fun onNonVisitorPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            return
        }
        if (player.hasPermission(PERMISSION_WHITELIST_SEE_VISITORS)) {
            return
        }
        server.onlinePlayers
            .filter { whitelist.isKnownVisitor(it.uniqueId) }
            .forEach { visitor ->
                player.hidePlayer(plugin, visitor)
            }
    }
}
