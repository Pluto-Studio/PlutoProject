package plutoproject.feature.paper.sitV2

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sitV2.*
import plutoproject.framework.paper.util.plugin
import kotlin.reflect.KClass

class SitImpl : Sit {
    override val allStrategies: Iterable<BlockSitStrategy>
        get() = strategies.keys
    override val sittingPlayers: Iterable<Player>
        get() = sitContexts.keys

    private val armorStandMarkerKey = NamespacedKey(plugin, "sit.armor_stand_marker")
    private val sitContexts = mutableMapOf<Player, SitContext>()
    private val strategies = mutableMapOf<BlockSitStrategy, Int>()

    override fun getState(player: Player): SitState {
        val context = sitContexts[player] ?: return SitState.NOT_SITTING
        if (context.block != null && context.armorStand != null) {
            return SitState.ON_BLOCK
        }
        if (context.targetPlayer != null) {
            return SitState.ON_PLAYER
        }
        error("Unexpected")
    }

    override fun getSittingBlock(player: Player): Block? {
        if (!getState(player).isSittingOnBlock) {
            return null
        }
        return sitContexts[player]!!.block
    }

    override fun getSittingPlayer(player: Player): Player? {
        if (!getState(player).isSittingOnPlayer) {
            return null
        }
        return sitContexts[player]!!.targetPlayer
    }

    override fun getOptions(player: Player): SitOptions? {
        return sitContexts[player]?.options
    }

    override fun sitOnBlock(sitter: Player, target: Block, sitOptions: SitOptions): SitResult {
        TODO("Not yet implemented")
    }

    override fun sitOnBlock(sitter: Player, target: Location, sitOptions: SitOptions): SitResult {
        TODO("Not yet implemented")
    }

    override fun isTemporaryArmorStand(entity: Entity): Boolean {
        return entity.persistentDataContainer.has(armorStandMarkerKey)
    }

    override fun registerStrategy(strategy: BlockSitStrategy, priority: Int): Boolean {
        if (strategies.keys.any { it::class == strategy::class }) {
            return false
        }
        strategies[strategy] = priority
        return true
    }

    override fun unregisterStrategy(strategyClass: KClass<out BlockSitStrategy>): Boolean {
        if (!strategies.keys.any { it::class == strategyClass }) {
            return false
        }
        strategies.entries.removeIf { it.key::class == strategyClass }
        return true
    }

    override fun getStrategy(block: Block): BlockSitStrategy? {
        return strategies.entries
            .sortedBy { it.value }
            .firstOrNull { it.key.match(block) }?.key
    }

    override fun getPriority(strategyClass: KClass<out BlockSitStrategy>): Int? {
        val strategy = strategies.keys.firstOrNull { it::class == strategyClass } ?: return null
        return strategies[strategy] ?: error("Unexpected")
    }

    override fun isStrategyRegistered(strategyClass: KClass<out BlockSitStrategy>): Boolean {
        return strategies.keys.any { it::class == strategyClass }
    }
}
