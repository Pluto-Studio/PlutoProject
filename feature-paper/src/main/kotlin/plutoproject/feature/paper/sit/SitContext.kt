package plutoproject.feature.paper.sit

import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.SitOptions

data class SitContext(
    val block: Block?,
    val targetPlayer: Player?,
    val armorStand: ArmorStand?,
    val options: SitOptions
)
