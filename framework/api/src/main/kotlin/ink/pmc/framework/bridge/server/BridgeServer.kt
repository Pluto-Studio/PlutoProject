package ink.pmc.framework.bridge.server

import ink.pmc.framework.bridge.player.PlayerLookup
import ink.pmc.framework.bridge.world.BridgeWorld

interface BridgeServer : PlayerLookup, GroupElement {
    val id: String
    val type: ServerType
    val state: ServerState
    val worlds: Collection<BridgeWorld>
    val isOnline: Boolean
    val isLocal: Boolean get() = state == ServerState.LOCAL
    val isRemoteBackend: Boolean get() = state == ServerState.REMOTE && type == ServerType.BACKEND
    val isRemoteProxy: Boolean get() = state == ServerState.REMOTE && type == ServerType.PROXY

    fun getWorld(name: String): BridgeWorld? {
        return worlds.firstOrNull { it.name == name }
    }

    fun isWorldExisted(name: String): Boolean {
        return getWorld(name) != null
    }
}