package plutoproject.feature.sit.paper.player.contexts

import org.bukkit.entity.AreaEffectCloud
import plutoproject.feature.sit.api.paper.SitOptions
import plutoproject.feature.sit.api.paper.player.PlayerStack

data class PassengerSitContext(
    override val stack: PlayerStack,
    val seatEntity: AreaEffectCloud,
    val options: SitOptions
) : PlayerSitContext
