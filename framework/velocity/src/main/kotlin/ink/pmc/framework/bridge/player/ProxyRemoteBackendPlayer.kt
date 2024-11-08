package ink.pmc.framework.bridge.player

import com.velocitypowered.api.proxy.Player
import ink.pmc.advkt.component.RootComponentKt
import ink.pmc.advkt.sound.SoundKt
import ink.pmc.advkt.title.ComponentTitleKt
import ink.pmc.framework.bridge.BridgeLocationImpl
import ink.pmc.framework.bridge.BridgeRpc
import ink.pmc.framework.bridge.InternalPlayer
import ink.pmc.framework.bridge.proto.BridgeRpcOuterClass.PlayerOperationResult.ContentCase.*
import ink.pmc.framework.bridge.proto.playerOperation
import ink.pmc.framework.bridge.server.BridgeGroup
import ink.pmc.framework.bridge.server.BridgeServer
import ink.pmc.framework.bridge.server.ServerType
import ink.pmc.framework.bridge.server.localServer
import ink.pmc.framework.bridge.world.BridgeLocation
import ink.pmc.framework.bridge.world.BridgeWorld
import ink.pmc.framework.utils.concurrent.submitAsync
import ink.pmc.framework.utils.proto.empty
import kotlinx.coroutines.Deferred
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import java.util.*

class ProxyRemoteBackendPlayer(
    val actual: Player,
    override val server: BridgeServer,
) : InternalPlayer() {
    override val group: BridgeGroup? = server.group
    override val serverType: ServerType = ServerType.REMOTE_BACKEND
    override val uniqueId: UUID = actual.uniqueId
    override val name: String = actual.username
    override val location: Deferred<BridgeLocation>
        get() = submitAsync<BridgeLocation> {
            check(server.isOnline) { "Server offline" }
            val result = BridgeRpc.operatePlayer(playerOperation {
                id = UUID.randomUUID().toString()
                remoteBackend = true
                infoLookup = empty
            })
            val info = result.infoLookup.location
            val world = server.getWorld(info.world) ?: error("World not found: ${info.world}")
            when (result.contentCase!!) {
                OK -> BridgeLocationImpl(server, world, info.x, info.y, info.z, info.yaw, info.pitch)
                PLAYER_OFFLINE -> error("Player offline")
                SERVER_OFFLINE -> error("Server offline")
                WORLD_NOT_FOUND -> error("Unexpected")
                UNSUPPORTED -> error("Unsupported")
                TIMEOUT -> error("Timeout while looking-up $name's info")
                CONTENT_NOT_SET -> error("Unexpected")
            }
        }
    override var isOnline: Boolean = true

    private val local = localServer.getPlayer(uniqueId)!!

    override suspend fun teleport(location: BridgeLocation) {
        TODO("Not yet implemented")
    }

    override suspend fun teleport(world: BridgeWorld) {
        TODO("Not yet implemented")
    }

    override suspend fun sendMessage(message: String) {
        local.sendMessage(message)
    }

    override suspend fun sendMessage(message: Component) {
        local.sendMessage(message)
    }

    override suspend fun sendMessage(message: RootComponentKt.() -> Unit) {
        local.sendMessage(message)
    }

    override suspend fun showTitle(title: Title) {
        local.showTitle(title)
    }

    override suspend fun showTitle(title: ComponentTitleKt.() -> Unit) {
        local.showTitle(title)
    }

    override suspend fun playSound(sound: Sound) {
        local.playSound(sound)
    }

    override suspend fun playSound(sound: SoundKt.() -> Unit) {
        local.playSound(sound)
    }

    override suspend fun performCommand(command: String) {

    }

    override fun convertElement(type: ServerType): BridgePlayer? {
        if (type == serverType) return this
        if (type == ServerType.REMOTE_PROXY) return null
        return super.convertElement(type)
    }
}