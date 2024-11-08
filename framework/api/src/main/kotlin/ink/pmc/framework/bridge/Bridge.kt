package ink.pmc.framework.bridge

import ink.pmc.framework.bridge.player.BridgePlayer
import ink.pmc.framework.bridge.player.PlayerLookup
import ink.pmc.framework.bridge.server.BridgeGroup
import ink.pmc.framework.bridge.server.BridgeServer
import ink.pmc.framework.bridge.server.ServerLookup
import ink.pmc.framework.utils.inject.inlinedGet

interface Bridge : PlayerLookup, ServerLookup {
    companion object : Bridge by inlinedGet()

    val local: BridgeServer
    val master: BridgeServer
        get() = servers.first { it.id == "_master" }
    val groups: Collection<BridgeGroup>
    override val players: Collection<BridgePlayer>
        get() = servers.flatMap { it.players }

    fun getGroup(id: String): BridgeGroup?

    fun isGroupRegistered(id: String): Boolean
}