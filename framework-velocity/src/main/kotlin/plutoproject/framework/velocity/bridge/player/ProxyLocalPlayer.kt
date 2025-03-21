package plutoproject.framework.velocity.bridge.player

import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.Deferred
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.ResultWrapper
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.exception.PlayerOperationResultWrapper
import plutoproject.framework.common.bridge.player.InternalPlayer
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.velocity.util.switchServer
import java.util.*

class ProxyLocalPlayer(private val actual: Player, server: BridgeServer) : InternalPlayer() {
    override var server: BridgeServer = server
        set(_) = error("Unsupported")
    override val group: BridgeGroup? = Bridge.local.group
    override val serverType: ServerType = Bridge.local.type
    override val serverState: ServerState = Bridge.local.state
    override val uniqueId: UUID = actual.uniqueId
    override val name: String = actual.username
    override val location: Deferred<ResultWrapper<BridgeLocation>>
        get() = convertElement(ServerState.REMOTE, ServerType.BACKEND)?.location ?: error("Unsupported")
    override var world: BridgeWorld?
        get() = convertElement(ServerState.REMOTE, ServerType.BACKEND)?.world ?: error("Unsupported")
        set(_) = error("Unsupported")
    override var isOnline: Boolean
        get() = actual.isActive
        set(_) {}

    override suspend fun teleport(location: BridgeLocation): ResultWrapper<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.teleport(location) ?: error("Unsupported")

    override suspend fun sendMessage(message: Component): ResultWrapper<Unit> {
        actual.sendMessage(message)
        return PlayerOperationResultWrapper(Unit, PlayerOperationResult.StatusCase.OK, name, server.id, false)
    }

    override suspend fun showTitle(title: Title): ResultWrapper<Unit> {
        actual.showTitle(title)
        return PlayerOperationResultWrapper(Unit, PlayerOperationResult.StatusCase.OK, name, server.id, false)
    }

    override suspend fun playSound(sound: Sound): ResultWrapper<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.playSound(sound) ?: error("Unsupported")

    override suspend fun performCommand(command: String): ResultWrapper<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.performCommand(command) ?: error("Unsupported")

    override suspend fun switchServer(server: String): ResultWrapper<Unit> {
        actual.switchServer(server)
        return PlayerOperationResultWrapper(Unit, PlayerOperationResult.StatusCase.OK, name, this.server.id, false)
    }
}
