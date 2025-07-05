package plutoproject.feature.paper.api.sit.block

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.BlockSitFinalResult
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.framework.common.util.inject.Koin
import kotlin.reflect.KClass

interface BlockSit {
    companion object : BlockSit by Koin.get()

    val allStrategies: Collection<BlockSitStrategy>
    val sitters: Collection<Player>

    fun isSitting(player: Player): Boolean

    fun getSeat(player: Player): Block?

    fun getSitter(seat: Block): Player?

    fun getSitter(seat: Location): Player?

    fun getOptions(player: Player): SitOptions?

    fun sit(player: Player, target: Block, sitOptions: SitOptions = SitOptions(), cause: SitOnBlockCause = SitOnBlockCause.PLUGIN): BlockSitFinalResult

    fun sit(player: Player, target: Location, sitOptions: SitOptions = SitOptions(), cause: SitOnBlockCause = SitOnBlockCause.PLUGIN): BlockSitFinalResult

    fun standUp(player: Player, cause: StandUpFromBlockCause = StandUpFromBlockCause.PLUGIN): Boolean

    fun isTemporarySeatEntity(entity: Entity): Boolean

    fun registerStrategy(strategy: BlockSitStrategy, priority: Int = 0): Boolean

    fun unregisterStrategy(strategyClass: KClass<out BlockSitStrategy>): Boolean

    fun getStrategy(block: Block): BlockSitStrategy?

    fun getPriority(strategyClass: KClass<out BlockSitStrategy>): Int?

    fun isStrategyRegistered(strategyClass: KClass<out BlockSitStrategy>): Boolean
}
