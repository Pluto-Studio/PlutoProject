package ink.pmc.framework.bridge

import ink.pmc.framework.bridge.player.BridgePlayer
import ink.pmc.framework.bridge.player.PlayerLookup
import ink.pmc.framework.bridge.server.BridgeGroup
import ink.pmc.framework.bridge.server.BridgeServer
import ink.pmc.framework.bridge.server.ServerLookup
import ink.pmc.framework.bridge.server.ServerType
import ink.pmc.framework.utils.inject.inlinedGet
import java.util.*

interface Bridge : PlayerLookup, ServerLookup {
    companion object : Bridge by inlinedGet()

    val local: BridgeServer
    val master: BridgeServer
        get() = servers.first { it.id == "_master" }
    val groups: Collection<BridgeGroup>
    override val players: Collection<BridgePlayer>
        get() = servers.flatMap { it.players }.sortedBy {
            when {
                it.isLocal -> 0
                it.isRemoteBackend -> 1
                it.isRemoteProxy -> 2
                else -> error("Unexpected")
            }
        }.distinctBy { it.uniqueId }

    fun getGroup(id: String): BridgeGroup?

    fun isGroupRegistered(id: String): Boolean

    override fun getPlayer(name: String, type: ServerType?): BridgePlayer? {
        if (type != null) {
            return servers.flatMap { it.players }.firstOrNull { it.name == name && it.serverType == type }
        }
        return super.getPlayer(name, null)
    }

    override fun getPlayer(uniqueId: UUID, type: ServerType?): BridgePlayer? {
        if (type != null) {
            return servers.flatMap { it.players }.firstOrNull { it.uniqueId == uniqueId && it.serverType == type }
        }
        return super.getPlayer(uniqueId, null)
    }

    override fun getRemotePlayer(name: String): BridgePlayer? {
        return servers.flatMap { it.players }.firstOrNull { it.name == name && !it.isLocal }
    }

    override fun getRemotePlayer(uniqueId: UUID): BridgePlayer? {
        return servers.flatMap { it.players }.firstOrNull { it.uniqueId == uniqueId && !it.isLocal }
    }
}