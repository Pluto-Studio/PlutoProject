package plutoproject.framework.common.api.bridge.player

import ink.pmc.advkt.component.RootComponentKt
import ink.pmc.advkt.sound.SoundKt
import ink.pmc.advkt.title.ComponentTitleKt
import kotlinx.coroutines.Deferred
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.api.bridge.world.WorldElement
import java.util.*

interface BridgePlayer : WorldElement<BridgePlayer> {
    val uniqueId: UUID
    val name: String
    val location: Deferred<Result<BridgeLocation>>
    val isOnline: Boolean

    suspend fun teleport(location: BridgeLocation): Result<Unit>

    suspend fun teleport(world: BridgeWorld) = teleport(world.spawnPoint)

    suspend fun teleport(player: BridgePlayer): Result<Unit> {
        val location = player.location.await()
        if (location.isFailure) {
            return Result.failure(location.exceptionOrNull()!!)
        }
        return teleport(location.getOrNull()!!)
    }

    suspend fun sendMessage(message: String) = sendMessage(Component.text(message))

    suspend fun sendMessage(message: Component): Result<Unit>

    suspend fun sendMessage(message: RootComponentKt.() -> Unit) = sendMessage(RootComponentKt().apply(message).build())

    suspend fun showTitle(title: Title): Result<Unit>

    suspend fun showTitle(title: ComponentTitleKt.() -> Unit) = showTitle(ComponentTitleKt().apply(title).build())

    suspend fun playSound(sound: Sound): Result<Unit>

    suspend fun playSound(sound: SoundKt.() -> Unit) = playSound(SoundKt().apply(sound).build())

    suspend fun performCommand(command: String): Result<Unit>

    suspend fun switchServer(server: String): Result<Unit>

    override fun convertElement(state: ServerState, type: ServerType): BridgePlayer? {
        if (serverState == state && serverType == type) return this
        return Bridge.servers.flatMap { it.players }.firstOrNull {
            it.uniqueId == it.uniqueId
                    && it.serverState == state
                    && it.serverType == type
        }
    }
}
