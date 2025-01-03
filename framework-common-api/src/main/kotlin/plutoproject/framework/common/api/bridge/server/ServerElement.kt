package plutoproject.framework.common.api.bridge.server

interface ServerElement<T : ServerElement<T>> : GroupElement {
    val server: BridgeServer
    val serverState: ServerState
        get() = server.state
    val serverType: ServerType
        get() = server.type
    override val group: BridgeGroup?
        get() = server.group

    fun convertElement(state: ServerState = serverState, type: ServerType = serverType): T?
}
