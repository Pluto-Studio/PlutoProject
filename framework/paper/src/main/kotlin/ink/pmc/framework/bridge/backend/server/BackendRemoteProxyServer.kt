package ink.pmc.framework.bridge.backend.server

import ink.pmc.framework.bridge.server.BridgeGroup
import ink.pmc.framework.bridge.server.InternalServer
import ink.pmc.framework.bridge.server.ServerState
import ink.pmc.framework.bridge.server.ServerType
import ink.pmc.framework.bridge.world.BridgeWorld

class BackendRemoteProxyServer : InternalServer() {
    override val id: String = "_master"
    override val group: BridgeGroup? = null
    override val state: ServerState = ServerState.REMOTE
    override val type: ServerType = ServerType.PROXY
    override val worlds: MutableSet<BridgeWorld>
        get() = error("Unsupported")
    override var isOnline: Boolean
        get() = true
        set(_) = error("Unsupported")
}