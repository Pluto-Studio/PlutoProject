package plutoproject.feature.velocity.whitelist_v2.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.luckperms.api.LuckPermsProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.velocity.whitelist_v2.*
import plutoproject.feature.whitelist_v2.api.VisitorRecordParams
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.infra.messaging.VISITOR_NOTIFICATION_TOPIC
import plutoproject.feature.whitelist_v2.infra.messaging.VisitorNotification
import plutoproject.framework.common.api.connection.CharonFlowConnection
import plutoproject.framework.common.api.connection.GeoIpConnection
import plutoproject.framework.common.util.coroutine.PluginScope
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

@Suppress("UNUSED")
object VisitorListener : KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val whitelist by inject<Whitelist>()
    private val knownVisitors by inject<KnownVisitors>()
    private val visitorSessions = ConcurrentHashMap<UUID, VisitorSession>()
    private val luckpermsApi = LuckPermsProvider.get()

    data class VisitorSession(
        val joinTime: Instant,
        val visitedServers: MutableSet<String> = mutableSetOf(),
        val ip: InetAddress,
        val virtualHost: InetSocketAddress?,
        var actionbarJob: Job? = null,
    )

    @Subscribe(priority = Short.MAX_VALUE)
    fun PlayerChooseInitialServerEvent.onPlayerChooseServer() {
        val player = this.player
        if (whitelist.isKnownVisitor(player.uniqueId)) {
            val welcomeMessage = if (shouldSendEnglishMessage(player)) {
                PLAYER_VISITOR_WELCOME_ENGLISH
            } else {
                PLAYER_VISITOR_WELCOME
            }
            player.sendMessage(welcomeMessage)
            featureLogger.info("访客 ${player.username} (${player.uniqueId}) 的客户端语言为 ${player.playerSettings.locale}。")

            val session = visitorSessions[player.uniqueId]
            if (session != null) {
                session.actionbarJob = startActionbarTask(player)
            }
        }
    }

    @Subscribe(priority = Short.MIN_VALUE)
    suspend fun ServerPreConnectEvent.onServerPreConnect() {
        if (!whitelist.isKnownVisitor(player.uniqueId)) {
            return
        }
        val server = originalServer.serverInfo.name
        val notification = VisitorNotification(
            uniqueId = player.uniqueId,
            username = player.username,
            joinedServer = server,
        )
        CharonFlowConnection.client.publish(VISITOR_NOTIFICATION_TOPIC, notification)
        featureLogger.info("通知后端：${player.username} (${player.uniqueId}), 连接至 $server")
    }

    private fun startActionbarTask(player: Player): Job {
        val isEnglish = shouldSendEnglishMessage(player)
        val actionbarMessage = if (isEnglish) PLAYER_VISITOR_ACTIONBAR_ENGLISH else PLAYER_VISITOR_ACTIONBAR

        return PluginScope.launch {
            while (whitelist.isKnownVisitor(player.uniqueId)) {
                player.sendActionBar(actionbarMessage)
                delay(1000)
            }
        }
    }

    private fun shouldSendEnglishMessage(player: Player): Boolean {
        val clientLocale = player.playerSettings.locale

        val isChineseLocale = clientLocale.language == "zh" ||
            clientLocale.toString() == "zh_CN" ||
            clientLocale.toString() == "zh_HK" ||
            clientLocale.toString() == "zh_TW"

        if (isChineseLocale) {
            return false
        }

        return try {
            val session = visitorSessions[player.uniqueId]
            if (session != null) {
                val response = GeoIpConnection.database.country(session.ip)
                val countryIso = response.country.isoCode
                val isInChina = countryIso == "CN" || countryIso == "HK" || countryIso == "MO" || countryIso == "TW"
                !isInChina
            } else {
                false
            }
        } catch (e: Exception) {
            false
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

    @Subscribe(priority = Short.MIN_VALUE)
    fun DisconnectEvent.onPlayerDisconnect() {
        val player = this.player
        if (!whitelist.isKnownVisitor(player.uniqueId)) {
            return
        }
        val session = visitorSessions.remove(player.uniqueId)
        if (session != null) {
            session.actionbarJob?.cancel()
            createVisitorRecord(player, session)
        }

        val user = luckpermsApi.userManager.getUser(player.uniqueId)
        if (user != null) {
            user.data().clear()
            luckpermsApi.userManager.saveUser(user)
        }
        featureLogger.info("已清除访客 ${player.username} (${player.uniqueId}) 的 LuckPerms 数据。")
        knownVisitors.remove(player.uniqueId)
        featureLogger.info("访客退出: ${player.username} (${player.uniqueId})")
    }

    fun recordVisitorSession(uuid: UUID, ip: InetAddress, virtualHost: InetSocketAddress?) {
        visitorSessions[uuid] = VisitorSession(
            joinTime = Instant.now(),
            ip = ip,
            virtualHost = virtualHost,
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
                visitedServers = session.visitedServers,
            )
        )
    }
}
