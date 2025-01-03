package plutoproject.framework.common.bridge.server

import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.player.BridgePlayer
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.BridgeServer

data class BridgeGroupImpl(override val id: String) : BridgeGroup {
    override val players: Collection<BridgePlayer>
        get() = Bridge.players.filter { it.group == this }
    override val servers: Collection<BridgeServer>
        get() = Bridge.servers.filter { it.group == this }
}
