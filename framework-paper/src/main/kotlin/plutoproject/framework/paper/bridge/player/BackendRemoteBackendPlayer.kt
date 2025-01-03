package plutoproject.framework.paper.bridge.player

import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.checkPlayerOperationResult
import plutoproject.framework.common.bridge.player.RemoteBackendPlayer
import plutoproject.framework.paper.bridge.bridgeStub
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import java.util.*

open class BackendRemoteBackendPlayer(
    override val uniqueId: UUID,
    override val name: String,
    override var server: BridgeServer,
    override var world: BridgeWorld?
) : RemoteBackendPlayer() {
    override suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult {
        val result = bridgeStub.operatePlayer(request)
        checkPlayerOperationResult(request, result)
        return result
    }
}
