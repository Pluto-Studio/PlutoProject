package plutoproject.feature.paper.sitV2

import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.SitOptions

data class SitContext(
    val block: Block?,
    val targetPlayer: Player?,
    val armorStand: ArmorStand?,
    val options: SitOptions
)
