package plutoproject.feature.paper.sit.block

import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import plutoproject.feature.paper.api.sit.SitOptions

data class BlockSitContext(
    val block: Block,
    val seatEntity: ArmorStand,
    val options: SitOptions
)
