package plutoproject.feature.whitelist.velocity.listeners

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
import net.luckperms.api.LuckPerms
import com.maxmind.geoip2.DatabaseReader
import club.plutoproject.charonflow.CharonFlow
import plutoproject.feature.whitelist.common.impl.KnownVisitors
import plutoproject.feature.whitelist.velocity.PLAYER_VISITOR_ACTIONBAR
import plutoproject.feature.whitelist.velocity.PLAYER_VISITOR_ACTIONBAR_ENGLISH
import plutoproject.feature.whitelist.velocity.getPlayerVisitorWelcome
import plutoproject.feature.whitelist.velocity.getPlayerVisitorWelcomeEnglish
import plutoproject.feature.whitelist.velocity.WhitelistConfig
import plutoproject.feature.whitelist.api.VisitorRecordParams
import plutoproject.feature.whitelist.api.WhitelistService
import plutoproject.feature.whitelist.common.VISITOR_NOTIFICATION_TOPIC
import plutoproject.feature.whitelist.common.VisitorNotification
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration
import java.util.logging.Logger
import plutoproject.capability.charonflow.api.CharonFlowConnection
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.kernel.api.koinGet
import plutoproject.feature.whitelist.velocity.moduleScope

@Suppress("UNUSED")
object VisitorListener {
    private val config = koinGet<WhitelistConfig>()
    private val service = koinGet<WhitelistService>()
    private val knownVisitors = koinGet<KnownVisitors>()
    private val charonFlow = koinGet<CharonFlowConnection>().client
    private val geoIpDatabase = koinGet<GeoIpConnection>().database
    private val logger = koinGet<Logger>()
    private val luckpermsApi: LuckPerms = LuckPermsProvider.get()
    private val visitorSessions = ConcurrentHashMap<UUID, VisitorSession>()
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
        if (service.isKnownVisitor(player.uniqueId)) {
            val welcomeMessage = if (shouldSendEnglishMessage(player)) {
                getPlayerVisitorWelcomeEnglish(config)
            } else {
                getPlayerVisitorWelcome(config)
            }
            player.sendMessage(welcomeMessage)
            logger.info("访客 ${player.username} (${player.uniqueId}) 的客户端语言为 ${player.playerSettings.locale}。")

            val session = visitorSessions[player.uniqueId]
            if (session != null) {
                session.actionbarJob = startActionbarTask(player)
            }
        }
    }

    @Subscribe(priority = Short.MIN_VALUE)
    suspend fun ServerPreConnectEvent.onServerPreConnect() {
        if (!service.isKnownVisitor(player.uniqueId)) {
            return
        }
        val server = originalServer.serverInfo.name
        val notification = VisitorNotification(
            uniqueId = player.uniqueId,
            username = player.username,
            joinedServer = server,
        )
        charonFlow.publish(VISITOR_NOTIFICATION_TOPIC, notification)
        logger.info("通知后端：${player.username} (${player.uniqueId}), 连接至 $server")
    }

    private fun startActionbarTask(player: Player): Job {
        val isEnglish = shouldSendEnglishMessage(player)
        val actionbarMessage = if (isEnglish) PLAYER_VISITOR_ACTIONBAR_ENGLISH else PLAYER_VISITOR_ACTIONBAR

        return moduleScope.launch {
            while (service.isKnownVisitor(player.uniqueId)) {
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
                val response = geoIpDatabase.country(session.ip)
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
        if (service.isKnownVisitor(player.uniqueId)) {
            val session = visitorSessions[player.uniqueId]
            session?.visitedServers?.add(server.serverInfo.name)
        }
    }

    @Subscribe(priority = Short.MIN_VALUE)
    fun DisconnectEvent.onPlayerDisconnect() {
        val player = this.player
        if (!service.isKnownVisitor(player.uniqueId)) {
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
        logger.info("已清除访客 ${player.username} (${player.uniqueId}) 的 LuckPerms 数据。")
        knownVisitors.remove(player.uniqueId)
        logger.info("访客退出: ${player.username} (${player.uniqueId})")
    }

    fun recordVisitorSession(uuid: UUID, ip: InetAddress, virtualHost: InetSocketAddress?) {
        visitorSessions[uuid] = VisitorSession(
            joinTime = Instant.now(),
            ip = ip,
            virtualHost = virtualHost,
        )
    }

    private fun createVisitorRecord(player: Player, session: VisitorSession) = moduleScope.launch {
        val duration = JavaDuration.between(session.joinTime, Instant.now()).toKotlinDuration()
        service.createVisitorRecord(
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
