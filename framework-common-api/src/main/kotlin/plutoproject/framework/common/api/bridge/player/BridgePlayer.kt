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
    val location: Deferred<BridgeLocation>
    val isOnline: Boolean

    suspend fun teleport(location: BridgeLocation)

    suspend fun teleport(world: BridgeWorld) {
        teleport(world.spawnPoint)
    }

    suspend fun teleport(player: BridgePlayer) {
        teleport(player.location.await())
    }

    suspend fun sendMessage(message: String) {
        sendMessage(Component.text(message))
    }

    suspend fun sendMessage(message: Component)

    suspend fun sendMessage(message: RootComponentKt.() -> Unit) {
        sendMessage(RootComponentKt().apply(message).build())
    }

    suspend fun showTitle(title: Title)

    suspend fun showTitle(title: ComponentTitleKt.() -> Unit) {
        showTitle(ComponentTitleKt().apply(title).build())
    }

    suspend fun playSound(sound: Sound)

    suspend fun playSound(sound: SoundKt.() -> Unit) {
        playSound(SoundKt().apply(sound).build())
    }

    suspend fun performCommand(command: String)

    suspend fun switchServer(server: String)

    override fun convertElement(state: ServerState, type: ServerType): BridgePlayer? {
        if (serverState == state && serverType == type) return this
        return Bridge.servers.flatMap { it.players }.firstOrNull {
            it.uniqueId == it.uniqueId
                    && it.serverState == state
                    && it.serverType == type
        }
    }
}
