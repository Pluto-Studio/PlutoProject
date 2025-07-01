package plutoproject.feature.paper.sit.strategies

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.BlockSitDirection
import plutoproject.feature.paper.api.sit.BlockSitStrategy

object PistonBlockSitStrategy : BlockSitStrategy {
    private val pistonParts = arrayOf(
        Material.PISTON,
        Material.STICKY_PISTON,
        Material.PISTON_HEAD,
        Material.MOVING_PISTON
    )

    override fun match(block: Block): Boolean {
        return pistonParts.contains(block.type)
    }

    // 活塞状态变化难以处理，所以禁止坐在活塞上
    override fun isAllowed(block: Block): Boolean {
        return false
    }

    override fun shouldSitOnRightClick(player: Player, block: Block): Boolean {
        return false
    }

    override fun getSitLocation(player: Player, block: Block): Location {
        error("Unexpected")
    }

    override fun getSitDirection(player: Player, block: Block): BlockSitDirection {
        error("Unexpected")
    }
}
