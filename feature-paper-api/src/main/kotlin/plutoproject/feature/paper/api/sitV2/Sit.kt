package plutoproject.feature.paper.api.sitV2

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.Koin
import kotlin.reflect.KClass

interface Sit {
    companion object : Sit by Koin.get()

    val allStrategies: Iterable<BlockSitStrategy>
    val sittingPlayers: Iterable<Player>

    fun getState(player: Player): SitState

    fun getSittingBlock(player: Player): Block?

    fun getSittingPlayer(player: Player): Player?

    fun getOptions(player: Player): SitOptions?

    fun sitOnBlock(sitter: Player, target: Block, sitOptions: SitOptions = SitOptions()): SitResult

    fun sitOnBlock(sitter: Player, target: Location, sitOptions: SitOptions = SitOptions()): SitResult

    fun isTemporaryArmorStand(entity: Entity): Boolean

    fun registerStrategy(strategy: BlockSitStrategy, priority: Int = 0): Boolean

    fun unregisterStrategy(strategyClass: KClass<out BlockSitStrategy>): Boolean

    fun getStrategy(block: Block): BlockSitStrategy?

    fun getPriority(strategyClass: KClass<out BlockSitStrategy>): Int?

    fun isStrategyRegistered(strategyClass: KClass<out BlockSitStrategy>): Boolean
}
