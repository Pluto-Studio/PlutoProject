package plutoproject.framework.velocity.bridge

import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.bridge.*
import plutoproject.framework.common.bridge.player.InternalPlayer
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerInfo
import plutoproject.framework.velocity.bridge.player.ProxyRemoteBackendPlayer
import plutoproject.framework.velocity.bridge.server.ProxyLocalServer
import kotlin.jvm.optionals.getOrNull
import plutoproject.framework.velocity.util.server as proxyServer

class ProxyBridge : InternalBridge() {
    override val local: BridgeServer = ProxyLocalServer()

    init {
        servers.add(local)
    }

    override fun createRemotePlayer(info: PlayerInfo, server: InternalServer?): InternalPlayer? {
        val actualServer = server ?: getInternalRemoteServer(info.server)
        if (actualServer == null) {
            warn { throwRemoteServerNotFound(info.server) }
            return null
        }
        val remoteWorld = internalBridge.getInternalRemoteWorld(actualServer, info.world.name)
        val proxyPlayer = proxyServer.getPlayer(info.uniqueId.convertToUuid()).getOrNull()
        if (proxyPlayer == null) {
            warn { throwLocalPlayerNotFound(info.name) }
            return null
        }
        return ProxyRemoteBackendPlayer(proxyPlayer, actualServer, remoteWorld)
    }
}
