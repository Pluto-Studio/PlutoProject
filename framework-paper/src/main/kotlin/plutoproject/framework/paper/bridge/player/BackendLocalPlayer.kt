package plutoproject.framework.paper.bridge.player

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.common.bridge.player.RemoteBackendPlayer
import plutoproject.framework.paper.bridge.bridgeStub
import plutoproject.framework.paper.bridge.world.BackendLocalWorld
import plutoproject.framework.paper.bridge.world.createBridge
import plutoproject.framework.paper.bridge.world.createBukkit
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.entity.teleportSuspend
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import java.util.*

class BackendLocalPlayer(private val actual: Player, server: BridgeServer) : RemoteBackendPlayer() {
    override val uniqueId: UUID = actual.uniqueId
    override val name: String = actual.name
    override var server: BridgeServer = server
        set(_) = error("Unsupported")
    override var world: BridgeWorld?
        get() = server.getWorld(actual.world.name) ?: BackendLocalWorld(actual.world, server)
        set(_) = error("Unsupported")
    override val location: Deferred<BridgeLocation>
        get() = CompletableDeferred(actual.location.createBridge())
    override var isOnline: Boolean
        get() = actual.isOnline
        set(_) {}

    override suspend fun teleport(location: BridgeLocation) {
        if (location.server == internalBridge.local) {
            actual.teleportSuspend(location.createBukkit())
            return
        }
        super.teleport(location)
    }

    override suspend fun sendMessage(message: Component) {
        actual.sendMessage(message)
    }

    override suspend fun showTitle(title: Title) {
        actual.showTitle(title)
    }

    override suspend fun playSound(sound: Sound) {
        actual.playSound(sound)
    }

    override suspend fun performCommand(command: String) {
        actual.withSync {
            actual.performCommand(command)
        }
    }

    override suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult {
        return bridgeStub.operatePlayer(request)
    }
}
