package plutoproject.feature.velocity.whitelist_v2

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import plutoproject.feature.common.api.whitelist_v2.VisitorRecordParams
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.whitelist_v2.WhitelistImpl
import plutoproject.framework.common.util.coroutine.PluginScope
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

@Suppress("UNUSED")
object VisitorListener : KoinComponent {
    private val whitelist by lazy { get<Whitelist>() as WhitelistImpl }
    private val visitorSessions = ConcurrentHashMap<UUID, VisitorSession>()

    data class VisitorSession(
        val joinTime: Instant,
        val visitedServers: MutableSet<String> = mutableSetOf(),
        val ip: InetAddress,
        val virtualHost: InetSocketAddress?
    )

    @Subscribe
    fun PlayerChooseInitialServerEvent.onPlayerChooseServer() {
        val player = this.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            val targetServer = initialServer.orElse(null)
            if (targetServer != null) {
                // TODO: 广播访客玩家 UUID 和即将连接的后端服务器信息给所有后端服务器
                // 广播内容：玩家 UUID, 目标服务器名称
            }
        }
    }

    @Subscribe
    fun ServerConnectedEvent.onServerConnected() {
        val player = this.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            val session = visitorSessions[player.uniqueId]
            session?.visitedServers?.add(server.serverInfo.name)
        }
    }

    @Subscribe
    fun DisconnectEvent.onPlayerDisconnect() {
        val player = this.player
        if (!whitelist.isKnownVisitor(player.uniqueId)) {
            return
        }
        val session = visitorSessions.remove(player.uniqueId)
        if (session != null) {
            createVisitorRecord(player, session)
        }
        whitelist.removeKnownVisitor(player.uniqueId)
    }

    fun recordVisitorSession(uuid: UUID, ip: InetAddress, virtualHost: InetSocketAddress?) {
        visitorSessions[uuid] = VisitorSession(
            joinTime = Instant.now(),
            ip = ip,
            virtualHost = virtualHost
        )
    }

    private fun createVisitorRecord(player: Player, session: VisitorSession) = PluginScope.launch {
        val duration = JavaDuration.between(session.joinTime, Instant.now()).toKotlinDuration()
        whitelist.createVisitorRecord(
            player.uniqueId,
            VisitorRecordParams(
                ipAddress = session.ip,
                virtualHost = session.virtualHost ?: InetSocketAddress.createUnresolved("unknown", 0),
                visitedAt = session.joinTime,
                duration = duration,
                visitedServers = session.visitedServers
            )
        )
    }
}
