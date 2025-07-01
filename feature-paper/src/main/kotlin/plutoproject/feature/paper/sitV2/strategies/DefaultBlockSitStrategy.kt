package plutoproject.feature.paper.sitV2.strategies

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.BlockSitDirection
import plutoproject.feature.paper.api.sitV2.BlockSitStrategy

object DefaultBlockSitStrategy : BlockSitStrategy {
    override fun match(block: Block): Boolean {
        return block.isSolid
    }

    override fun isAllowed(block: Block): Boolean {
        return true
    }

    override fun shouldSitOnRightClick(player: Player, block: Block): Boolean {
        return false
    }

    override fun getSitLocation(player: Player, block: Block): Location {
        return block.location.apply {
            add(0.5, 0.0, 0.5)
            y = block.boundingBox.maxY
        }
    }

    override fun getSitDirection(player: Player, block: Block): BlockSitDirection {
        return BlockSitDirection.fromYaw(player.yaw)
    }
}
