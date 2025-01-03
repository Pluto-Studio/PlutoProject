package plutoproject.framework.velocity.bridge

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.common.bridge.player.createInfoWithoutLocation
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.common.bridge.throwRemoteServerNotFound
import plutoproject.framework.common.bridge.warn
import plutoproject.framework.proto.bridge.notification
import plutoproject.framework.proto.bridge.playerDisconnect
import plutoproject.framework.velocity.bridge.player.ProxyLocalPlayer
import plutoproject.framework.velocity.bridge.player.ProxyRemoteBackendPlayer
import plutoproject.framework.velocity.bridge.server.localServer

object BridgePlayerListener {
    @Subscribe(order = PostOrder.FIRST)
    suspend fun LoginEvent.e() {
        val localPlayer = ProxyLocalPlayer(player, localServer)
        localServer.players.add(localPlayer)
        BridgeRpc.notify(notification {
            playerJoin = localPlayer.createInfoWithoutLocation()
        })
    }

    @Subscribe(order = PostOrder.FIRST)
    suspend fun ServerPreConnectEvent.e() {
        val current = internalBridge.getInternalRemoteServer(originalServer.serverInfo.name)
            ?: return warn { throwRemoteServerNotFound(originalServer.serverInfo.name) }
        val remotePlayer = internalBridge.getInternalRemoteBackendPlayer(player.uniqueId)
            ?: ProxyRemoteBackendPlayer(player, current, null)
        val previous = previousServer?.let { internalBridge.getInternalRemoteServer(it.serverInfo.name) }
        previous?.players?.remove(remotePlayer)
        remotePlayer.server = current
        current.players.add(remotePlayer)
        BridgeRpc.notify(notification {
            playerSwitchServer = remotePlayer.createInfoWithoutLocation()
        })
    }

    @Subscribe(order = PostOrder.LAST)
    suspend fun DisconnectEvent.e() {
        val uniqueId = player.uniqueId
        internalBridge.servers.forEach {
            val server = it as InternalServer
            server.players.removeIf { player -> player.uniqueId == uniqueId }
        }
        BridgeRpc.notify(notification {
            playerDisconnect = playerDisconnect {
                this.uniqueId = uniqueId.toString()
            }
        })
    }
}
