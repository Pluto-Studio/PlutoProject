package plutoproject.feature.paper.sit.strategies

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.BlockSitDirection
import plutoproject.feature.paper.api.sit.BlockSitStrategy

object CarpetBlockSitStrategy : BlockSitStrategy {
    override fun match(block: Block): Boolean {
        return block.type.name.lowercase().endsWith("_carpet")
    }

    override fun isAllowed(block: Block): Boolean {
        return true
    }

    override fun shouldSitOnRightClick(player: Player, block: Block): Boolean {
        return true
    }

    override fun getSitLocation(player: Player, block: Block): Location {
        return DefaultBlockSitStrategy.getSitLocation(player, block)
    }

    override fun getSitDirection(player: Player, block: Block): BlockSitDirection {
        return DefaultBlockSitStrategy.getSitDirection(player, block)
    }
}
