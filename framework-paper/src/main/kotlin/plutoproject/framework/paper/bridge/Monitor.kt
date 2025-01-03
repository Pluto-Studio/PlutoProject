package plutoproject.framework.paper.bridge

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import plutoproject.framework.common.api.bridge.RESERVED_MASTER_ID
import plutoproject.framework.common.api.rpc.RpcClient
import plutoproject.framework.common.bridge.*
import plutoproject.framework.common.bridge.player.createInfoWithoutLocation
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.flow.getValue
import plutoproject.framework.common.util.data.flow.setValue
import plutoproject.framework.common.util.time.currentTimestampMillis
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcGrpcKt.BridgeRpcCoroutineStub
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.*
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.DataSyncResult.StatusCase.*
import plutoproject.framework.proto.bridge.heartbeatMessage
import plutoproject.framework.proto.bridge.serverInfo
import java.util.logging.Level
import kotlin.time.Duration.Companion.seconds

private var isInitialized by MutableStateFlow(false)
private var isConnected by MutableStateFlow(false)
private var monitorJob: Job? = null
private var heartbeatJob: Job? = null
val bridgeStub = BridgeRpcCoroutineStub(RpcClient.channel)

private fun getCurrentServerInfo(): ServerInfo {
    return serverInfo {
        val localServer = internalBridge.local
        id = localServer.id
        localServer.group?.let { group = it.id }
        backend = true
        players.addAll(localServer.players.map { it.createInfoWithoutLocation() })
        worlds.addAll(localServer.worlds.map { it.createInfo() })
    }
}

private suspend fun registerServer() {
    debugInfo("Trying to register server")
    val result = bridgeStub.registerServer(getCurrentServerInfo())
    when (result.statusCase!!) {
        ServerRegistrationResult.StatusCase.OK -> {}
        ServerRegistrationResult.StatusCase.MISSING_FIELDS -> warn { throwMissingFields() }
        ServerRegistrationResult.StatusCase.STATUS_NOT_SET -> warn { throwStatusNotSet("ServerRegistrationResult") }
    }
    result.serversList.forEach {
        if (it.id == internalBridge.local.id) return@forEach
        internalBridge.registerRemoteServer(it)
    }
    isInitialized = true
    debugInfo("Server registered")
}

private fun markConnect() {
    isConnected = true
    if (internalBridge.getInternalRemoteServer(RESERVED_MASTER_ID) != null) {
        internalBridge.markRemoteServerOnline(RESERVED_MASTER_ID)
    }
}

private fun markDisconnect() {
    isConnected = false
    if (internalBridge.getInternalRemoteServer(RESERVED_MASTER_ID) != null) {
        internalBridge.markRemoteServerOffline(RESERVED_MASTER_ID)
    }
}

private suspend fun monitor() = runCatching {
    bridgeStub.monitorNotification(Empty).also {
        debugInfo("Trying to monitor")
        // 已初始化但从 Master 断开：可能是因为网络问题，同步一次数据防止不一致
        if (isInitialized && !isConnected) {
            debugInfo("Trying to sync data")
            val result = bridgeStub.syncData(getCurrentServerInfo())
            when (result.statusCase!!) {
                OK -> {}
                NOT_REGISTERED -> {
                    registerServer()
                    debugInfo("Data synced")
                    return@also
                }

                MISSING_FIELDS -> warn { throwMissingFields() }
                STATUS_NOT_SET -> warn { throwStatusNotSet("DataSyncResult") }
            }
            result.serversList.forEach {
                if (it.id == internalBridge.local.id) return@forEach
                internalBridge.syncData(it)
            }
            debugInfo("Data synced")
        }
        // 未初始化：注册服务器
        if (!isInitialized) {
            registerServer()
        }
        markConnect()
        logger.info("Bridge monitor connected successfully")
        debugInfo("Monitor connected")
    }.collect {
        handleNotification(it)
    }
}.onFailure {
    if (isConnected) {
        logger.log(Level.SEVERE, "Bridge monitor disconnected, wait 5s before retry", it)
        markDisconnect()
    } else {
        logger.log(Level.SEVERE, "Failed to connect Bridge monitor, wait 5s before retry", it)
    }
    delay(5.seconds)
}

private fun startMonitor() {
    monitorJob = runAsync {
        while (true) {
            monitor()
        }
    }
}

private suspend fun heartbeat() = runCatching {
    val result = bridgeStub.heartbeat(heartbeatMessage {
        server = internalBridge.local.id
    })
    when (result.statusCase!!) {
        HeartbeatResult.StatusCase.OK -> {}
        // Master 报告未注册：可能是因为 Master 重启了，重新初始化
        HeartbeatResult.StatusCase.NOT_REGISTERED -> if (isInitialized) {
            debugInfo("Master responded notRegistered")
            isInitialized = false
            markDisconnect()
        }

        HeartbeatResult.StatusCase.MISSING_FIELDS -> warn { throwMissingFields() }
        HeartbeatResult.StatusCase.STATUS_NOT_SET -> warn { throwStatusNotSet("HeartbeatResult") }
    }
}

private fun startHeartbeat() {
    heartbeatJob = runAsync {
        while (true) {
            delay(HEARTBEAT_SEND_INTERVAL_SECS.seconds)
            heartbeat()
        }
    }
}

fun startBridgeBackgroundTask() {
    startMonitor()
    startHeartbeat()
}

fun stopBridgeBackgroundTask() {
    monitorJob?.cancel()
    heartbeatJob?.cancel()
    monitorJob = null
    heartbeatJob = null
}

private suspend fun handleNotification(msg: Notification) {
    debugInfo("Notification received: $msg, $currentTimestampMillis")
    runCatching {
        NotificationHandler.Companion[msg.contentCase].handle(msg)
    }.onFailure {
        logger.log(Level.WARNING, "Exception caught while handling notification", it)
    }
}
