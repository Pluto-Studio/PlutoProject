package plutoproject.feature.paper.sitV2

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.*

class SitImpl : Sit {
    override val allStrategies: Iterable<BlockSitStrategy>
        get() = TODO("Not yet implemented")

    override fun getState(player: Player): SitState {
        TODO("Not yet implemented")
    }

    override fun getSittingBlock(player: Player): Block? {
        TODO("Not yet implemented")
    }

    override fun getSittingPlayer(player: Player): Player? {
        TODO("Not yet implemented")
    }

    override fun getOptions(player: Player): SitOptions? {
        TODO("Not yet implemented")
    }

    override fun sitOnBlock(sitter: Player, target: Block, sitOptions: SitOptions): SitResult {
        TODO("Not yet implemented")
    }

    override fun sitOnBlock(sitter: Player, target: Location, sitOptions: SitOptions): SitResult {
        TODO("Not yet implemented")
    }

    override fun registerStrategy(strategy: BlockSitStrategy, priority: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun unregisterStrategy(strategy: BlockSitStrategy): Boolean {
        TODO("Not yet implemented")
    }

    override fun getStrategy(block: Block): BlockSitStrategy? {
        TODO("Not yet implemented")
    }

    override fun getPriority(strategy: BlockSitStrategy): Int? {
        TODO("Not yet implemented")
    }

    override fun isStrategyRegistered(strategy: BlockSitStrategy): Boolean {
        TODO("Not yet implemented")
    }
}
