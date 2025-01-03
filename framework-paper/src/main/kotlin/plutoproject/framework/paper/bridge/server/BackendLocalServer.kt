package plutoproject.framework.paper.bridge.server

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.RESERVED_MASTER_ID
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.bridge.server.BridgeGroupImpl
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.common.config.BridgeConfig
import plutoproject.framework.paper.bridge.player.BackendLocalPlayer
import plutoproject.framework.paper.bridge.world.BackendLocalWorld
import plutoproject.framework.paper.util.server

internal inline val localServer: BackendLocalServer
    get() = Bridge.local as BackendLocalServer

class BackendLocalServer : InternalServer(), KoinComponent {
    private val config by inject<BridgeConfig>()
    override val id: String = config.id
    override val group: BridgeGroup? = config.group?.let { BridgeGroupImpl(it) }
    override val type: ServerType = ServerType.BACKEND
    override val state: ServerState = ServerState.LOCAL
    override var isOnline: Boolean = true
        set(_) = error("Unsupported")

    init {
        require(id != RESERVED_MASTER_ID) { "$RESERVED_MASTER_ID is an internal reserved ID" }
        worlds.addAll(server.worlds.map { BackendLocalWorld(it, this) })
        players.addAll(server.onlinePlayers.map { BackendLocalPlayer(it, this) })
    }
}
