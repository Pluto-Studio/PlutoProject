package plutoproject.feature.paper.sit

import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import plutoproject.feature.paper.api.sit.*
import plutoproject.feature.paper.api.sit.SitState.*
import plutoproject.feature.paper.api.sit.events.PlayerSitOnBlockEvent
import plutoproject.feature.paper.api.sit.events.PlayerStandUpFromBlockEvent
import plutoproject.feature.paper.api.sit.events.PlayerStandUpFromPlayerEvent
import plutoproject.feature.paper.sit.strategies.*
import plutoproject.framework.paper.util.plugin
import kotlin.math.max
import kotlin.reflect.KClass

class SitImpl : Sit {
    override val allStrategies: Iterable<BlockSitStrategy>
        get() = strategies.keys
    override val sittingPlayers: Iterable<Player>
        get() = sitContexts.keys

    private val armorStandMarkerKey = NamespacedKey(plugin, "sit.armor_stand_marker")
    private val sitContexts = mutableMapOf<Player, SitContext>()
    private val strategies = mutableMapOf(
        PistonBlockSitStrategy to Int.MIN_VALUE,
        SlabBlockSitStrategy to Int.MAX_VALUE - 1,
        StairBlockSitStrategy to Int.MAX_VALUE - 1,
        CampfireBlockSitStrategy to Int.MAX_VALUE - 1,
        ScaffoldingBlockSitStrategy to Int.MAX_VALUE - 1,
        DefaultBlockSitStrategy to Int.MAX_VALUE,
    )
    private val internalStrategyClasses = strategies.keys.map { it::class }

    override fun getState(player: Player): SitState {
        val context = sitContexts[player] ?: return NOT_SITTING
        if (context.block != null && context.armorStand != null) {
            return ON_BLOCK
        }
        if (context.targetPlayer != null) {
            return ON_PLAYER
        }
        error("Unexpected")
    }

    private fun createArmorStand(location: Location): ArmorStand {
        return location.world.spawn(location, ArmorStand::class.java).apply {
            setGravity(false)
            setAI(false)
            setCanTick(false)
            setCanMove(false)
            isInvulnerable = true
            isInvisible = true
            isCollidable = false
            persistentDataContainer.set(armorStandMarkerKey, PersistentDataType.BOOLEAN, true)
        }
    }

    private fun removeArmorStand(sitter: Player) {
        val armorStand = sitContexts[sitter]?.armorStand!!
        armorStand.removePassenger(sitter)
        armorStand.remove()
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

    override fun getSitterOn(block: Block): Player? {
        return sitContexts.entries.firstOrNull { it.value.block == block }?.key
    }

    override fun getSitterOn(blockLocation: Location): Player? {
        return getSitterOn(blockLocation.block)
    }

    override fun gitSitterOn(player: Player): Player? {
        return sitContexts.entries.firstOrNull { it.value.targetPlayer == player }?.key
    }

    override fun getOptions(player: Player): SitOptions? {
        return sitContexts[player]?.options
    }

    private fun callSitOnBlockEvent(
        sitter: Player,
        options: SitOptions,
        attemptResult: SitAttemptResult,
        sittingOn: Block,
        strategy: BlockSitStrategy?
    ): PlayerSitOnBlockEvent {
        return PlayerSitOnBlockEvent(sitter, options, attemptResult, sittingOn, strategy).apply { callEvent() }
    }

    private fun callPlayerStandUpFromBlockEvent(sitter: Player): PlayerStandUpFromBlockEvent {
        return PlayerStandUpFromBlockEvent(
            sitter,
            getOptions(sitter)!!,
            getSittingBlock(sitter)!!
        ).apply { callEvent() }
    }

    private fun callPlayerStandUpFromPlayerEvent(sitter: Player): PlayerStandUpFromPlayerEvent {
        return PlayerStandUpFromPlayerEvent(
            sitter,
            getOptions(sitter)!!,
            getSittingPlayer(sitter)!!
        ).apply { callEvent() }
    }

    private fun Player.playSitSound() {
        val leggings = inventory.leggings
        val sound = if (leggings == null) {
            Sound.ITEM_ARMOR_EQUIP_GENERIC
        } else when (leggings.type) {
            Material.LEATHER_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_LEATHER
            Material.CHAINMAIL_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_CHAIN
            Material.IRON_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_IRON
            Material.GOLDEN_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_GOLD
            Material.DIAMOND_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_DIAMOND
            Material.NETHERITE_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_NETHERITE
            else -> Sound.ITEM_ARMOR_EQUIP_GENERIC
        }
        world.playSound(location, sound, SoundCategory.BLOCKS, 1f, 1f)
    }

    override fun sitOnBlock(sitter: Player, target: Block, sitOptions: SitOptions): SitFinalResult {
        check(Bukkit.isPrimaryThread()) { "Sit operation can only be performed on main thread." }

        if (getState(sitter).isSitting) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_ALREADY_SITTING, target, null)
            return SitFinalResult.FAILED_ALREADY_SITTING
        }
        if (sitContexts.values.any { it.block == target }) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_TARGET_OCCUPIED, target, null)
            return SitFinalResult.FAILED_TARGET_OCCUPIED
        }

        val bodyBlock1 = target.location.clone().add(0.0, 1.0, 0.0).block
        val bodyBlock2 = target.location.clone().add(0.0, 2.0, 0.0).block

        if (bodyBlock1.isCollidable || bodyBlock2.isCollidable) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_TARGET_BLOCKED_BY_BLOCKS, target, null)
            return SitFinalResult.FAILED_TARGET_BLOCKED_BY_BLOCKS
        }

        val strategy = getStrategy(target)

        if (strategy == null || !strategy.isAllowed(target)) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_INVALID_TARGET, target, null)
            return SitFinalResult.FAILED_INVALID_TARGET
        }

        val sitLocation = strategy.getSitLocation(sitter, target)
        val sitDirection = strategy.getSitDirection(sitter, target)
        val event = callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.SUCCEED, target, strategy)

        if (event.isCancelled) {
            return SitFinalResult.FAILED_CANCELLED_BY_PLUGIN
        }

        val armorStandLocation = sitLocation.clone().apply {
            subtract(0.0, 2.0, 0.0)
            yaw = sitDirection.yaw
        }
        val armorStand = createArmorStand(armorStandLocation)

        if (sitOptions.playSitSound) {
            sitter.playSitSound()
        }

        sitContexts[sitter] = SitContext(target, null, armorStand, sitOptions)
        armorStand.addPassenger(sitter)

        return SitFinalResult.SUCCEED
    }

    override fun sitOnBlock(sitter: Player, target: Location, sitOptions: SitOptions): SitFinalResult {
        return sitOnBlock(sitter, target.block, sitOptions)
    }

    override fun sitOnPlayer(sitter: Player, target: Player, sitOptions: SitOptions): SitFinalResult {
        TODO("Not yet implemented")
    }

    override fun standUp(sitter: Player): Boolean {
        check(Bukkit.isPrimaryThread()) { "Stand up operation can only be performed on main thread." }

        val state = getState(sitter)
        val sitContext = sitContexts[sitter]!!
        val standUpLocation = when (state) {
            NOT_SITTING -> return false
            ON_BLOCK -> sitter.location.clone().apply {
                // 某些方块（MOVING_PISTON）的顶面高度不太正常...
                val maxY = max(sitContext.block!!.boundingBox.maxY, sitContext.block.location.y)
                y = maxY + 0.5
            }

            ON_PLAYER -> sitter.location
        }

        if (state.isSittingOnBlock && !callPlayerStandUpFromBlockEvent(sitter).isCancelled) {
            removeArmorStand(sitter)
        } else if (state.isSittingOnPlayer && !callPlayerStandUpFromPlayerEvent(sitter).isCancelled) {
            getSittingPlayer(sitter)?.removePassenger(sitter)
        } else {
            return false
        }

        if (getOptions(sitter)!!.playSitSound) {
            sitter.playSitSound()
        }

        sitter.teleport(standUpLocation)
        sitContexts.remove(sitter)
        sitter.sendActionBar(Component.empty())

        return true
    }

    override fun isTemporaryArmorStand(entity: Entity): Boolean {
        return entity.persistentDataContainer.has(armorStandMarkerKey)
    }

    override fun registerStrategy(strategy: BlockSitStrategy, priority: Int): Boolean {
        require(priority >= 0) { "Priority must be greater or equal to 0." }
        if (strategies.keys.any { it::class == strategy::class }) {
            return false
        }
        strategies[strategy] = priority
        return true
    }

    override fun unregisterStrategy(strategyClass: KClass<out BlockSitStrategy>): Boolean {
        require(!internalStrategyClasses.contains(strategyClass)) { "Internal strategy cannot be unregistered." }
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
