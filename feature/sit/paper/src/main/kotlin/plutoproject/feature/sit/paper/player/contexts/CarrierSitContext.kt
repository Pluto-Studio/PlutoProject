package plutoproject.feature.sit.paper.player.contexts

import plutoproject.feature.sit.api.paper.player.PlayerStack

data class CarrierSitContext(override val stack: PlayerStack) : PlayerSitContext
