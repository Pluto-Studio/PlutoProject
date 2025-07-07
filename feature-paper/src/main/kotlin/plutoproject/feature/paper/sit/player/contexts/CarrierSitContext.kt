package plutoproject.feature.paper.sit.player.contexts

import plutoproject.feature.paper.api.sit.player.PlayerStack

data class CarrierSitContext(override val stack: PlayerStack) : PlayerSitContext
