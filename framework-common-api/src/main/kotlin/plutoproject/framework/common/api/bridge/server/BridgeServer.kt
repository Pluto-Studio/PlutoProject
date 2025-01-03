package plutoproject.framework.common.api.bridge.server

import plutoproject.framework.common.api.bridge.player.PlayerLookup
import plutoproject.framework.common.api.bridge.world.BridgeWorld

interface BridgeServer : PlayerLookup, GroupElement {
    val id: String
    val state: ServerState
    val type: ServerType
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
