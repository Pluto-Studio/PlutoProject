package plutoproject.framework.common.bridge.server

import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.player.createInfoWithoutLocation
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.ServerInfo
import plutoproject.framework.proto.bridge.serverInfo

fun BridgeServer.createInfo(): ServerInfo {
    val server = this
    val localType = Bridge.local.type
    return serverInfo {
        this.id = server.id
        server.group?.id?.also { this.group = it }
        when {
            server.isLocal -> if (localType == ServerType.PROXY) proxy = true else backend = true
            server.isRemoteBackend -> backend = true
            server.isRemoteProxy -> if (localType == ServerType.PROXY) error("Unexpected") else proxy = true
        }
        players.addAll(server.players.map { it.createInfoWithoutLocation() })
        if (!type.isProxy) {
            worlds.addAll(server.worlds.map { it.createInfo() })
        }
    }
}

abstract class InternalServer : BridgeServer {
    override val players: MutableSet<BridgePlayer> = mutableConcurrentSetOf()
    override val worlds: MutableSet<BridgeWorld> = mutableConcurrentSetOf()
    abstract override var isOnline: Boolean

    override fun equals(other: Any?): Boolean {
        if (other !is BridgeServer) return false
        return other.id == id && other.state == state && other.type == type
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
