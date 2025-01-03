package plutoproject.framework.common.bridge.server

import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType

class RemoteBackendServer(override val id: String, override val group: BridgeGroup?) : InternalServer() {
    override val type: ServerType = ServerType.BACKEND
    override val state: ServerState = ServerState.REMOTE
    override var isOnline: Boolean = true
}
