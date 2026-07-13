package plutoproject.feature.sit.paper.block

import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import plutoproject.feature.sit.api.paper.SitOptions

data class BlockSitContext(
    val block: Block,
    val seatEntity: ArmorStand,
    val options: SitOptions
)
