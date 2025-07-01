package plutoproject.feature.paper.sitV2.strategies

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.BlockSitDirection
import plutoproject.feature.paper.api.sitV2.BlockSitStrategy

object SolidBlockSitStrategy : BlockSitStrategy {
    override fun match(block: Block) = block.isSolid

    override fun isAllowed(player: Player, block: Block) = true

    override fun getSitLocation(player: Player, block: Block): Location = block.location.apply {
        add(0.5, 0.0, 0.5)
        y = block.boundingBox.maxY
    }

    override fun getSitDirection(player: Player, block: Block) = BlockSitDirection.fromYaw(player.yaw)
}
