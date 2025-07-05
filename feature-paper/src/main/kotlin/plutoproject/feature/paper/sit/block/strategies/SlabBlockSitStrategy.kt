package plutoproject.feature.paper.sit.block.strategies

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.block.BlockSitDirection
import plutoproject.feature.paper.api.sit.block.BlockSitStrategy

object SlabBlockSitStrategy : BlockSitStrategy {
    override fun match(block: Block): Boolean {
        return block.blockData is Slab
    }

    override fun isAllowed(block: Block): Boolean {
        return true
    }

    override fun shouldSitOnRightClick(player: Player, block: Block): Boolean {
        val slab = block.blockData as Slab
        return slab.type != Slab.Type.DOUBLE
    }

    override fun getSitLocation(player: Player, block: Block): Location {
        return DefaultBlockSitStrategy.getSitLocation(player, block)
    }

    override fun getSitDirection(player: Player, block: Block): BlockSitDirection {
        return DefaultBlockSitStrategy.getSitDirection(player, block)
    }
}
