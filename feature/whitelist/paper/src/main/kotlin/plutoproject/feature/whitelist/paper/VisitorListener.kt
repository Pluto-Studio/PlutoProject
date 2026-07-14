package plutoproject.feature.whitelist.paper

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
import plutoproject.feature.whitelist.common.impl.KnownVisitors
import plutoproject.feature.whitelist.api.WhitelistService
import java.util.logging.Logger
import plutoproject.kernel.api.koinGet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED")
object VisitorListener : Listener {
    private val service = koinGet<WhitelistService>()
    private val knownVisitors = koinGet<KnownVisitors>()
    private val logger = koinGet<Logger>()
    private val pendingVisitors = ConcurrentHashMap<UUID, Job>()

    fun onVisitorIncoming(uniqueId: UUID, username: String) {
        knownVisitors.add(uniqueId)

        val timeoutJob = moduleScope.launch {
            delay(30.seconds)
            if (service.isKnownVisitor(uniqueId)) {
                knownVisitors.remove(uniqueId)
                logger.warning("待处理的访客 $username ($uniqueId) 超时未连接")
            }
            pendingVisitors.remove(uniqueId)
        }

        pendingVisitors[uniqueId] = timeoutJob
        logger.info("收到访客进入通知: $username ($uniqueId)")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (service.isKnownVisitor(player.uniqueId)) {
            pendingVisitors.remove(player.uniqueId)?.cancel()
            player.gameMode = GameMode.SPECTATOR
            hideVisitorFromAllPlayers(player)
            logger.info("访客进入: ${player.name} (${player.uniqueId})")
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (service.isKnownVisitor(player.uniqueId)) {
            val spawn = warpManager?.getDefaultSpawn()?.location ?: player.world.spawnLocation
            player.teleport(spawn)
            player.gameMode = GameMode.SURVIVAL
            knownVisitors.remove(player.uniqueId)
            pendingVisitors.remove(player.uniqueId)?.cancel()
            logger.info("访客退出: ${player.name} (${player.uniqueId})")
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
                if (service.isKnownVisitor(otherPlayer.uniqueId)) {
                    visitor.hidePlayer(plugin, otherPlayer)
                }
            }
    }

    @EventHandler
    fun onNonVisitorPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (service.isKnownVisitor(player.uniqueId)) {
            return
        }
        if (player.hasPermission(PERMISSION_WHITELIST_SEE_VISITORS)) {
            return
        }
        server.onlinePlayers
            .filter { service.isKnownVisitor(it.uniqueId) }
            .forEach { visitor ->
                player.hidePlayer(plugin, visitor)
            }
    }
}
