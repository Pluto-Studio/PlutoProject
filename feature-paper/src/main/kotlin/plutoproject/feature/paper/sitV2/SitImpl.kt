package plutoproject.feature.paper.sitV2

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.*
import kotlin.reflect.KClass

class SitImpl : Sit {
    override val allStrategies: Iterable<BlockSitStrategy>
        get() = TODO("Not yet implemented")

    private val registeredStrategies = mutableMapOf<KClass<out BlockSitStrategy>, BlockSitStrategy>()
    private val strategyPriorityMap = mutableMapOf<BlockSitStrategy, Int>()

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
        if (registeredStrategies.containsKey(strategy::class)) {
            return false
        }
        registeredStrategies[strategy::class] = strategy
        strategyPriorityMap[strategy] = priority
        return true
    }

    override fun unregisterStrategy(strategyClass: KClass<out BlockSitStrategy>): Boolean {
        if (!registeredStrategies.containsKey(strategyClass)) {
            return false
        }
        val strategy = registeredStrategies[strategyClass] ?: error("Unexpected")
        registeredStrategies.remove(strategyClass)
        strategyPriorityMap.remove(strategy)
        return true
    }

    override fun getStrategy(block: Block): BlockSitStrategy? =
        strategyPriorityMap.entries
            .sortedBy { it.value }
            .firstOrNull { it.key.match(block) }?.key

    override fun getPriority(strategyClass: KClass<out BlockSitStrategy>): Int? {
        val strategy = registeredStrategies[strategyClass] ?: return null
        return strategyPriorityMap[strategy] ?: error("Unexpected")
    }

    override fun isStrategyRegistered(strategyClass: KClass<out BlockSitStrategy>) =
        registeredStrategies.containsKey(strategyClass)
}
