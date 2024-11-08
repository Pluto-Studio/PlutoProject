package ink.pmc.framework.bridge.world

import ink.pmc.framework.bridge.Bridge
import ink.pmc.framework.bridge.player.PlayerLookup
import ink.pmc.framework.bridge.server.ServerElement
import ink.pmc.framework.bridge.server.ServerType

interface BridgeWorld : PlayerLookup, ServerElement<BridgeWorld> {
    val name: String
    val alias: String?
    val aliasOrName: String get() = alias ?: name
    val spawnPoint: BridgeLocation

    fun getLocation(
        x: Double,
        y: Double,
        z: Double,
        yaw: Float = 0.0F,
        pitch: Float = 0.0F
    ): BridgeLocation

    override fun convertElement(type: ServerType): BridgeWorld? {
        return Bridge.servers.flatMap { it.worlds }
            .firstOrNull { it.name == name && it.server.id == server.id && it.serverType == type }
    }
}