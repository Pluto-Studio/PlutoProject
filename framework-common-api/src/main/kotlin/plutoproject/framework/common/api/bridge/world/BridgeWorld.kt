package plutoproject.framework.common.api.bridge.world

import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.api.bridge.player.PlayerLookup
import plutoproject.framework.common.api.bridge.server.ServerElement
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType

interface BridgeWorld : PlayerLookup, ServerElement<BridgeWorld> {
    val name: String
    val alias: String?
    val aliasOrName: String get() = alias ?: name
    val spawnPoint: BridgeLocation
    override val players: Collection<BridgePlayer>
        get() = server.players.filter { it.world == this }

    fun getLocation(
        x: Double,
        y: Double,
        z: Double,
        yaw: Float = 0.0F,
        pitch: Float = 0.0F
    ): BridgeLocation

    override fun convertElement(state: ServerState, type: ServerType): BridgeWorld? {
        if (serverState == state && serverType == type) return this
        return Bridge.servers.flatMap { it.worlds }
            .firstOrNull {
                it.name == name
                        && it.server.id == server.id
                        && it.serverState == state
                        && it.serverType == type
            }
    }
}
