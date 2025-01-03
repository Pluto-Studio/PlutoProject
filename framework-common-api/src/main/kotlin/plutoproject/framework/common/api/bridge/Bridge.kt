package plutoproject.framework.common.api.bridge

import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.api.bridge.player.PlayerLookup
import plutoproject.framework.common.api.bridge.server.*
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.util.inject.inlinedGet
import java.util.*

interface Bridge : PlayerLookup, ServerLookup {
    companion object : Bridge by inlinedGet()

    val local: BridgeServer
    val master: BridgeServer
        get() = servers.first { it.id == RESERVED_MASTER_ID }
    val groups: Collection<BridgeGroup>
        get() = servers.filter { it.group != null }.map { it.group!! }
    val worlds: Collection<BridgeWorld>
        get() = servers.filter { !it.type.isProxy }.flatMap { it.worlds }
    override val players: Collection<BridgePlayer>
        get() = servers.flatMap { it.players }.sortedBy {
            when {
                it.serverState.isLocal -> if (local.type.isProxy) 1 else 0
                it.serverState.isRemote && it.serverType.isBackend -> if (local.type.isProxy) 0 else 1
                it.serverState.isRemote && it.serverType.isProxy -> 2
                else -> error("Unexpected")
            }
        }.distinctBy { it.uniqueId }

    fun getGroup(id: String): BridgeGroup? {
        return groups.firstOrNull { it.id == id }
    }

    fun isGroupRegistered(id: String): Boolean {
        return getGroup(id) != null
    }

    private fun filterPlayer(state: ServerState?, type: ServerType?): List<BridgePlayer> {
        return servers.flatMap { it.players }.filter {
            it.serverState == (state ?: it.serverState) && it.serverType == (type ?: it.serverType)
        }
    }

    override fun getPlayer(name: String, state: ServerState?, type: ServerType?): BridgePlayer? {
        if (state != null || type != null) {
            return filterPlayer(state, type).firstOrNull { it.name == name }
        }
        return super.getPlayer(name, null, null)
    }

    override fun getPlayer(uniqueId: UUID, state: ServerState?, type: ServerType?): BridgePlayer? {
        if (state != null || type != null) {
            return filterPlayer(state, type).firstOrNull { it.uniqueId == uniqueId }
        }
        return super.getPlayer(uniqueId, null, null)
    }

    fun getWorld(server: BridgeServer, name: String): BridgeWorld? {
        return worlds.firstOrNull { it.server == server && it.name == name }
    }
}
