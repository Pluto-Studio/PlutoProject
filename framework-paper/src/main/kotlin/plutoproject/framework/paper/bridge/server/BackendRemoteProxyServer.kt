package plutoproject.framework.paper.bridge.server

import plutoproject.framework.common.api.bridge.RESERVED_MASTER_ID
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.server.InternalServer

class BackendRemoteProxyServer : InternalServer() {
    override val id: String = RESERVED_MASTER_ID
    override val group: BridgeGroup? = null
    override val state: ServerState = ServerState.REMOTE
    override val type: ServerType = ServerType.PROXY
    override val worlds: MutableSet<BridgeWorld>
        get() = error("Unsupported")
    override var isOnline: Boolean = true
}
