package plutoproject.feature.paper.sit.player.contexts

import org.bukkit.entity.AreaEffectCloud
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.player.PlayerStack

data class PassengerSitContext(
    override val stack: PlayerStack,
    val seatEntity: AreaEffectCloud,
    val options: SitOptions
) : PlayerSitContext
