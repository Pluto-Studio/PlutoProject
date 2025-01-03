package plutoproject.framework.velocity.bridge

import com.google.protobuf.Empty
import ink.pmc.advkt.title.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
import net.kyori.adventure.text.minimessage.MiniMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.bridge.*
import plutoproject.framework.common.bridge.player.InternalPlayer
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.common.bridge.server.createInfo
import plutoproject.framework.common.config.BridgeConfig
import plutoproject.framework.common.util.coroutine.PlutoCoroutineScope
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.common.util.data.flow.NoReplayNotifyFlow
import plutoproject.framework.proto.bridge.*
import plutoproject.framework.proto.bridge.BridgeRpcGrpcKt.BridgeRpcCoroutineImplBase
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.*
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation.ContentCase.*
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationAck.StatusCase.*
import plutoproject.framework.velocity.bridge.player.ProxyRemoteBackendPlayer
import plutoproject.framework.velocity.bridge.server.localServer
import plutoproject.framework.velocity.util.switchServer
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object BridgeRpc : BridgeRpcCoroutineImplBase(), KoinComponent {
    private val config by inject<BridgeConfig>()
    private var isRunning = false
    private val heartbeatMap = ConcurrentHashMap<BridgeServer, Instant>()
    private val heartbeatCheckJob: Job
    private val playerOperationAckNotify = NoReplayNotifyFlow<PlayerOperationAck>()
    private val notify = NoReplayNotifyFlow<Notification>()

    override fun monitorNotification(request: Empty): Flow<Notification> {
        debugInfo("BridgeRpc - monitorNotification called")
        notify
        return notify
    }

    suspend fun notify(notification: Notification) {
        notify.emit(notification)
    }

    private suspend fun checkHeartbeat(server: BridgeServer) {
        if (server.state.isLocal) return
        val remoteServer = server as InternalServer
        val time = heartbeatMap[server]
        val requirement = Instant.now().minusSeconds(HEARTBEAT_THRESHOLD_SECS + HEARTBEAT_GRACE_PERIOD_SECS)
        if (time == null || time.isBefore(requirement)) {
            if (remoteServer.isOnline) {
                internalBridge.markRemoteServerOffline(remoteServer.id)
                logger.warning("Server ${remoteServer.id} heartbeat timeout")
                notify.emit(notification {
                    serverOffline = server.id
                })
            }
        }
    }

    private suspend fun <T> rpcCatching(block: suspend () -> T): T {
        return runCatching {
            block()
        }.onFailure {
            logger.log(Level.SEVERE, "BridgeRpc - Internal error", it)
        }.getOrThrow()
    }

    init {
        isRunning = true
        // Heartbeat check loop
        heartbeatCheckJob = runAsync {
            while (isRunning) {
                delay(HEARTBEAT_CHECK_INTERVAL_SECS.seconds)
                internalBridge.servers.forEach {
                    checkHeartbeat(it)
                }
            }
        }
    }

    override suspend fun registerServer(request: ServerInfo): ServerRegistrationResult = rpcCatching {
        debugInfo("BridgeRpc - registerRemoteServer called: $request")
        if (request.id == "" || !(request.hasProxy() || request.hasBackend())) {
            return@rpcCatching serverRegistrationResult { missingFields = true }
        }
        val server = internalBridge.registerRemoteServer(request)
        heartbeatMap[server] = Instant.now()
        notify.emit(notification {
            serverRegistration = request
        })
        logger.info("A server registered successfully: ${request.id} (${request.playersCount} players, ${request.worldsCount} worlds)")
        return@rpcCatching serverRegistrationResult {
            ok = true
            servers.addAll(internalBridge.servers.map { it.createInfo() })
        }
    }

    private suspend fun markAlive(remoteServer: InternalServer) {
        heartbeatMap[remoteServer] = Instant.now()
        if (!remoteServer.isOnline) {
            internalBridge.markRemoteServerOnline(remoteServer.id)
            notify.emit(notification {
                serverOnline = remoteServer.id
            })
        }
    }

    override suspend fun heartbeat(request: HeartbeatMessage): HeartbeatResult = rpcCatching {
        debugInfo("BridgeRpc - heartbeat called: $request")
        if (request.server == "") {
            return@rpcCatching heartbeatResult { missingFields = true }
        }
        val remoteServer = internalBridge.getServer(request.server) as InternalServer?
            ?: return@rpcCatching heartbeatResult {
                notRegistered = true
            }
        markAlive(remoteServer)
        return@rpcCatching heartbeatResult {
            ok = true
        }
    }

    override suspend fun syncData(request: ServerInfo): DataSyncResult = rpcCatching {
        debugInfo("BridgeRpc - syncData called: $request")
        if (request.id == "" || !(request.proxy || request.backend)) {
            return@rpcCatching dataSyncResult { missingFields = true }
        }
        if (internalBridge.getInternalRemoteServer(request.id) == null) {
            return@rpcCatching dataSyncResult {
                notRegistered = true
            }
        }
        val remoteServer = internalBridge.syncData(request)
        markAlive(remoteServer)
        notify.emit(notification {
            serverInfoUpdate = request
        })
        return@rpcCatching dataSyncResult {
            ok = true
            servers.addAll(internalBridge.servers.map { it.createInfo() })
        }
    }

    private fun handleNoReturnAck(ack: PlayerOperationAck): PlayerOperationResult {
        return when (ack.statusCase!!) {
            OK -> playerOperationResult { ok = true }
            UNSUPPORTED -> playerOperationResult { unsupported = true }
            MISSING_FIELDS -> playerOperationResult { missingFields = true }
            STATUS_NOT_SET -> throwStatusNotSet("PlayerOperationAck")
        }
    }

    private suspend fun waitAck(
        request: PlayerOperation,
        then: (PlayerOperationAck) -> PlayerOperationResult
    ): PlayerOperationResult {
        for (ack in playerOperationAckNotify.produceIn(PlutoCoroutineScope)) {
            if (ack.id != request.id) continue
            return then(ack)
        }
        error("PlayerOperationAck channel closed unexpectedly")
    }

    private suspend fun handleInfoLookup(request: PlayerOperation): PlayerOperationResult =
        withTimeoutOrNull(config.operationTimeout) {
            notify.emit(notification { playerOperation = request })
            waitAck(request) {
                when (it.statusCase!!) {
                    OK -> playerOperationResult {
                        ok = true
                        infoLookup = it.infoLookup
                    }

                    UNSUPPORTED -> playerOperationResult { unsupported = true }
                    MISSING_FIELDS -> playerOperationResult { missingFields = true }
                    STATUS_NOT_SET -> playerOperationResult { missingFields = true }
                }
            }
        } ?: playerOperationResult { timeout = true }

    private suspend fun handleSendMessage(request: PlayerOperation, player: InternalPlayer): PlayerOperationResult {
        player.sendMessage(MiniMessage.miniMessage().deserialize(request.sendMessage))
        return playerOperationResult { ok = true }
    }

    private suspend fun handleSendTitle(request: PlayerOperation, player: InternalPlayer): PlayerOperationResult {
        player.showTitle {
            val info = request.showTitle
            times {
                fadeIn(info.fadeInMs.milliseconds)
                stay(info.stayMs.milliseconds)
                fadeOut(info.fadeOutMs.milliseconds)
            }
            mainTitle(MiniMessage.miniMessage().deserialize(info.mainTitle))
            subTitle(MiniMessage.miniMessage().deserialize(info.subTitle))
        }
        return playerOperationResult { ok = true }
    }

    private suspend fun handlePlaySound(request: PlayerOperation) = withTimeoutOrNull(config.operationTimeout) {
        if (internalBridge.getInternalRemoteBackendPlayer(request.playerUuid.convertToUuid()) == null) {
            return@withTimeoutOrNull playerOperationResult {
                unsupported = true
            }
        }
        notify.emit(notification { playerOperation = request })
        waitAck(request, BridgeRpc::handleNoReturnAck)
    } ?: playerOperationResult {
        timeout = true
    }

    private suspend fun handleTeleport(request: PlayerOperation) = withTimeoutOrNull(config.operationTimeout) {
        val remotePlayer = internalBridge.getInternalRemoteBackendPlayer(request.playerUuid.convertToUuid())
                as ProxyRemoteBackendPlayer?
            ?: return@withTimeoutOrNull playerOperationResult {
                unsupported = true
            }
        val currentServerInfo = remotePlayer.actual.currentServer.getOrNull()?.serverInfo
        if (currentServerInfo?.name != request.teleport.server) {
            remotePlayer.actual.switchServer(request.teleport.server)
        }
        notify.emit(notification { playerOperation = request })
        waitAck(request, BridgeRpc::handleNoReturnAck)
    } ?: playerOperationResult {
        timeout = true
    }

    private suspend fun handlePerformCommand(request: PlayerOperation) = withTimeoutOrNull(config.operationTimeout) {
        internalBridge.getInternalRemoteBackendPlayer(request.playerUuid.convertToUuid())
            ?: return@withTimeoutOrNull playerOperationResult { unsupported = true }
        notify.emit(notification { playerOperation = request })
        waitAck(request, BridgeRpc::handleNoReturnAck)
    } ?: playerOperationResult {
        timeout = true
    }

    private suspend fun handleSwitchServer(request: PlayerOperation, player: InternalPlayer): PlayerOperationResult {
        runCatching {
            player.switchServer(request.switchServer)
        }.onFailure {
            return playerOperationResult { serverOffline = true }
        }
        return playerOperationResult { ok = true }
    }

    override suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult = rpcCatching {
        debugInfo("BridgeRpc - operatePlayer called: $request")
        if (request.id == ""
            || request.executor == ""
            || request.playerUuid == ""
            || request.contentCase == CONTENT_NOT_SET
        ) {
            return@rpcCatching playerOperationResult { missingFields = true }
        }
        val localPlayer = localServer.getPlayer(request.playerUuid.convertToUuid(), ServerState.LOCAL, ServerType.PROXY)
                as InternalPlayer? ?: return@rpcCatching playerOperationResult { playerOffline = true }
        return@rpcCatching when (request.contentCase!!) {
            INFO_LOOKUP -> handleInfoLookup(request)
            SEND_MESSAGE -> handleSendMessage(request, localPlayer)
            SHOW_TITLE -> handleSendTitle(request, localPlayer)
            PLAY_SOUND -> handlePlaySound(request)
            TELEPORT -> handleTeleport(request)
            PERFORM_COMMAND -> handlePerformCommand(request)
            SWITCH_SERVER -> handleSwitchServer(request, localPlayer)
            CONTENT_NOT_SET -> throwContentNotSet("PlayerOperation")
        }
    }

    override suspend fun ackPlayerOperation(request: PlayerOperationAck): CommonResult = rpcCatching {
        debugInfo("BridgeRpc - ackPlayerOperation called: $request")
        if (request.id == ""
            || !(request.hasOk() || request.hasUnsupported() || request.hasMissingFields())
        ) {
            return@rpcCatching commonResult { missingFields = true }
        }
        playerOperationAckNotify.emit(request)
        commonResult { ok = true }
    }

    override suspend fun updatePlayerInfo(request: PlayerInfo): CommonResult = rpcCatching {
        debugInfo("BridgeRpc - updatePlayerInfo called: $request")
        if (request.server == ""
            || request.uniqueId == ""
            || request.name == ""
            || !(request.hasProxy() || request.hasBackend())
        ) {
            return@rpcCatching commonResult { missingFields = true }
        }
        internalBridge.updateRemotePlayerInfo(request)
        notify.emit(notification { playerInfoUpdate = request })
        commonResult { ok = true }
    }

    override suspend fun operateWorld(request: WorldOperation): WorldOperationResult = rpcCatching {
        debugInfo("BridgeRpc - operateWorld called: $request")
        if (request.id == ""
            || request.server == ""
            || request.world == ""
        ) {
            return@rpcCatching worldOperationResult { missingFields = true }
        }
        worldOperationResult { ok = true }
    }

    private fun checkWorldInfo(info: WorldInfo): Boolean =
        !(info.server == "" || info.name == "" || !info.hasSpawnPoint())

    override suspend fun ackWorldOperation(request: WorldOperationAck): CommonResult = rpcCatching {
        debugInfo("BridgeRpc - ackWorldOperation called: $request")
        if (request.id == ""
            || request.server == ""
            || request.world == ""
            || !(request.hasOk() || request.hasUnsupported() || request.hasMissingFields())
        ) {
            return@rpcCatching commonResult { missingFields = true }
        }
        commonResult { ok = true }
    }

    override suspend fun loadWorld(request: WorldInfo): CommonResult = rpcCatching {
        debugInfo("BridgeRpc - loadWorld called: $request")
        if (!checkWorldInfo(request)) {
            return@rpcCatching commonResult { missingFields = true }
        }
        internalBridge.addRemoteWorld(request)
        notify.emit(notification {
            worldLoad = request
        })
        commonResult { ok = true }
    }

    override suspend fun updateWorldInfo(request: WorldInfo): CommonResult = rpcCatching {
        debugInfo("BridgeRpc - updateRemoteWorldInfo called: $request")
        if (!checkWorldInfo(request)) {
            return@rpcCatching commonResult { missingFields = true }
        }
        internalBridge.updateRemoteWorldInfo(request)
        notify.emit(notification { worldInfoUpdate = request })
        commonResult { ok = true }
    }

    override suspend fun unloadWorld(request: WorldLoad): CommonResult = rpcCatching {
        debugInfo("BridgeRpc - unloadWorld called: $request")
        if (request.world == "" || request.world == "") {
            return@rpcCatching commonResult { missingFields = true }
        }
        internalBridge.removeRemoteWorld(request)
        notify.emit(notification { worldUnload = request })
        commonResult { ok = true }
    }
}