package plutoproject.framework.common.bridge.player

import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerInfo
import plutoproject.framework.proto.bridge.playerInfo

fun BridgePlayer.createInfoWithoutLocation(): PlayerInfo {
    val player = this
    return playerInfo {
        server = player.server.id
        uniqueId = player.uniqueId.toString()
        name = player.name
        when {
            player.serverType.isProxy -> proxy = true
            else -> backend = true
        }
        if (!player.serverType.isProxy) {
            player.world?.also { world = it.createInfo() }
        }
    }
}

abstract class InternalPlayer : BridgePlayer {
    abstract override var server: BridgeServer
    abstract override var world: BridgeWorld?
    override var isOnline: Boolean = true

    override fun equals(other: Any?): Boolean {
        if (other !is BridgePlayer) return false
        return other.uniqueId == uniqueId
                && other.serverState == serverState
                && other.serverType == serverType
    }

    override fun hashCode(): Int {
        var result = uniqueId.hashCode()
        result = 31 * result + serverState.hashCode()
        result = 31 * result + serverType.hashCode()
        return result
    }
}

