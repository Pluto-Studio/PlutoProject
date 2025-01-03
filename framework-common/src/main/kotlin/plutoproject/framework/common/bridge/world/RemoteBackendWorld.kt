package plutoproject.framework.common.bridge.world

import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeLocation

class RemoteBackendWorld(
    override val server: BridgeServer,
    override val name: String,
    override val alias: String?,
) : InternalWorld() {
    override val serverType: ServerType = ServerType.BACKEND
    override val serverState: ServerState = ServerState.REMOTE
    override val group: BridgeGroup? = server.group
    override lateinit var spawnPoint: BridgeLocation

    override fun getLocation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float): BridgeLocation =
        BridgeLocationImpl(server, this, x, y, z, yaw, pitch)
}
