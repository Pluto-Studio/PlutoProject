package plutoproject.framework.velocity.bridge.server

import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.RESERVED_MASTER_ID
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.velocity.util.server
import java.util.*

internal inline val localServer: ProxyLocalServer
    get() = Bridge.local as ProxyLocalServer

class ProxyLocalServer : InternalServer() {
    override val group: BridgeGroup? = null
    override val id: String = RESERVED_MASTER_ID
    override val type: ServerType = ServerType.PROXY
    override val state: ServerState = ServerState.LOCAL
    override val worlds: MutableSet<BridgeWorld>
        get() = error("Unsupported")
    override var isOnline: Boolean = true
        set(_) = error("Unsupported")

    override fun getWorld(name: String): BridgeWorld? {
        error("Unsupported")
    }

    override fun isWorldExisted(name: String): Boolean {
        error("Unsupported")
    }

    override fun isPlayerOnline(name: String): Boolean {
        return server.getPlayer(name).isPresent
    }

    override fun isPlayerOnline(uniqueId: UUID): Boolean {
        return server.getPlayer(uniqueId).isPresent
    }
}
