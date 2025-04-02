package plutoproject.framework.paper.bridge.player

import kotlinx.coroutines.Deferred
import net.kyori.adventure.sound.Sound
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.player.RemotePlayer
import plutoproject.framework.paper.bridge.bridgeStub
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import java.util.*

class BackendRemoteProxyPlayer(
    override val uniqueId: UUID,
    override val name: String,
    override var server: BridgeServer
) : RemotePlayer() {
    override var world: BridgeWorld?
        get() = convertElement(ServerState.REMOTE, ServerType.BACKEND)?.world ?: error("Unsupported")
        set(_) = error("Unsupported")
    override val location: Deferred<Result<BridgeLocation>>
        get() = convertElement(ServerState.REMOTE, ServerType.BACKEND)?.location ?: error("Unsupported")

    override suspend fun teleport(location: BridgeLocation): Result<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.teleport(location) ?: error("Unsupported")

    override suspend fun playSound(sound: Sound) =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.playSound(sound) ?: error("Unsupported")

    override suspend fun performCommand(command: String): Result<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.performCommand(command) ?: error("Unsupported")

    override suspend fun switchServer(server: String): Result<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.switchServer(server) ?: error("Unsupported")

    override suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult {
        val result = bridgeStub.operatePlayer(request)
        return result
    }
}
