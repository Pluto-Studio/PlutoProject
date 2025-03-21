package plutoproject.framework.velocity.bridge.player

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.ResultWrapper
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.exception.PlayerOperationResultWrapper
import plutoproject.framework.common.bridge.player.RemoteBackendPlayer
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.velocity.bridge.BridgeRpc
import java.util.*

class ProxyRemoteBackendPlayer(
    val actual: Player,
    override var server: BridgeServer,
    override var world: BridgeWorld?
) : RemoteBackendPlayer() {
    override val uniqueId: UUID = actual.uniqueId
    override val name: String = actual.username
    override var isOnline: Boolean
        get() = actual.isActive
        set(_) {}

    override suspend fun sendMessage(message: Component): ResultWrapper<Unit> {
        actual.sendMessage(message)
        return PlayerOperationResultWrapper(Unit, PlayerOperationResult.StatusCase.OK, name, server.id, false)
    }

    override suspend fun showTitle(title: Title): ResultWrapper<Unit> {
        actual.showTitle(title)
        return PlayerOperationResultWrapper(Unit, PlayerOperationResult.StatusCase.OK, name, server.id, false)
    }

    override suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult {
        return BridgeRpc.operatePlayer(request)
    }
}
