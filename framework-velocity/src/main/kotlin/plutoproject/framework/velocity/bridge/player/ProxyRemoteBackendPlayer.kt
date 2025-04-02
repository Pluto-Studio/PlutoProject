package plutoproject.framework.velocity.bridge.player

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.world.BridgeWorld
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

    override suspend fun sendMessage(message: Component): Result<Unit> {
        actual.sendMessage(message)
        return Result.success(Unit)
    }

    override suspend fun showTitle(title: Title): Result<Unit> {
        actual.showTitle(title)
        return Result.success(Unit)
    }

    override suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult {
        return BridgeRpc.operatePlayer(request)
    }
}
