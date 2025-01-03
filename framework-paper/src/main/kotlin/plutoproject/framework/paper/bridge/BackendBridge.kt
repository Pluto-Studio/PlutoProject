package plutoproject.framework.paper.bridge

import plutoproject.framework.common.api.bridge.RESERVED_MASTER_ID
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.bridge.InternalBridge
import plutoproject.framework.common.bridge.player.InternalPlayer
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.common.bridge.throwRemoteServerNotFound
import plutoproject.framework.common.bridge.warn
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.paper.bridge.player.BackendRemoteBackendPlayer
import plutoproject.framework.paper.bridge.player.BackendRemoteProxyPlayer
import plutoproject.framework.paper.bridge.server.BackendLocalServer
import plutoproject.framework.paper.bridge.server.BackendRemoteProxyServer
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerInfo
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.ServerInfo

class BackendBridge : InternalBridge() {
    override val local: BridgeServer = BackendLocalServer()

    override fun createRemotePlayer(info: PlayerInfo, server: InternalServer?): InternalPlayer? {
        val remoteServer = server ?: getInternalRemoteServer(info.server)
        if (remoteServer == null) {
            warn { throwRemoteServerNotFound(info.server) }
            return null
        }
        val remoteWorld = if (!remoteServer.type.isProxy) {
            getInternalRemoteWorld(remoteServer, info.world.name)
        } else {
            null
        }
        return if (info.proxy) {
            BackendRemoteProxyPlayer(info.uniqueId.convertToUuid(), info.name, remoteServer)
        } else {
            BackendRemoteBackendPlayer(info.uniqueId.convertToUuid(), info.name, remoteServer, remoteWorld)
        }
    }

    override fun createRemoteServer(info: ServerInfo): InternalServer {
        if (info.id == RESERVED_MASTER_ID) {
            val server = BackendRemoteProxyServer().apply {
                setInitialPlayers(info, this)
            }
            return server
        }
        return super.createRemoteServer(info)
    }

    init {
        servers.add(local)
    }
}
