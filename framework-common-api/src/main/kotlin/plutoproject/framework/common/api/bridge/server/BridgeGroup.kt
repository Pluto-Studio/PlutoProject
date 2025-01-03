package plutoproject.framework.common.api.bridge.server

import plutoproject.framework.common.api.bridge.player.PlayerLookup

interface BridgeGroup : PlayerLookup, ServerLookup {
    val id: String
}
