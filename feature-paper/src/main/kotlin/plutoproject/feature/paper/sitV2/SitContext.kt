package plutoproject.feature.paper.sitV2

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.SitOptions

data class SitContext(
    val blockLocation: Location?,
    val targetPlayer: Player?,
    val armorStand: ArmorStand?,
    val options: SitOptions
)
