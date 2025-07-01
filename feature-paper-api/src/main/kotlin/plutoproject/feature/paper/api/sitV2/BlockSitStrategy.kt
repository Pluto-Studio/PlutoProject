package plutoproject.feature.paper.api.sitV2

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player

interface BlockSitStrategy {
    fun match(block: Block): Boolean

    fun isAllowed(player: Player, block: Block): Boolean

    fun sitOnRightClick(player: Player, block: Block): Boolean

    fun getSitLocation(player: Player, block: Block): Location

    fun getSitDirection(player: Player, block: Block): BlockSitDirection
}
