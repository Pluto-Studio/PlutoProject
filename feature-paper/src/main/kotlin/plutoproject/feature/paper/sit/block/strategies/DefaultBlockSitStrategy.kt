package plutoproject.feature.paper.sit.block.strategies

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.block.BlockSitDirection
import plutoproject.feature.paper.api.sit.block.BlockSitStrategy
import kotlin.math.max

object DefaultBlockSitStrategy : BlockSitStrategy {
    override fun match(block: Block): Boolean {
        return block.isCollidable
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
            y = max(block.location.y, block.boundingBox.maxY) // 某些方块 (MOVING_PISTON) 的顶面高度返回 0
        }
    }

    override fun getSitDirection(player: Player, block: Block): BlockSitDirection {
        return BlockSitDirection.fromYaw(player.yaw)
    }
}
