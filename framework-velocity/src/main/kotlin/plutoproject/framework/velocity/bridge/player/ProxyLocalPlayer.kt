package plutoproject.framework.velocity.bridge.player

import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.UnsupportedException
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.player.InternalPlayer
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
    override val location: Deferred<Result<BridgeLocation>>
        get() = convertElement(ServerState.REMOTE, ServerType.BACKEND)?.location
            ?: CompletableDeferred(Result.failure(UnsupportedException()))
    override var world: BridgeWorld?
        get() = convertElement(ServerState.REMOTE, ServerType.BACKEND)?.world ?: error("Unsupported")
        set(_) = error("Unsupported")
    override var isOnline: Boolean
        get() = actual.isActive
        set(_) {}

    override suspend fun teleport(location: BridgeLocation): Result<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.teleport(location) ?: error("Unsupported")

    override suspend fun sendMessage(message: Component): Result<Unit> {
        actual.sendMessage(message)
        return Result.success(Unit)
    }

    override suspend fun showTitle(title: Title): Result<Unit> {
        actual.showTitle(title)
        return Result.success(Unit)
    }

    override suspend fun playSound(sound: Sound): Result<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.playSound(sound)
            ?: Result.failure(UnsupportedException())

    override suspend fun performCommand(command: String): Result<Unit> =
        convertElement(ServerState.REMOTE, ServerType.BACKEND)?.performCommand(command)
            ?: Result.failure(UnsupportedException())

    override suspend fun switchServer(server: String): Result<Unit> {
        actual.switchServer(server)
        return Result.success(Unit)
    }
}
