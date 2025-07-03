package plutoproject.feature.paper.sit.block

import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import plutoproject.feature.paper.api.sit.*
import plutoproject.feature.paper.api.sit.block.BlockSit
import plutoproject.feature.paper.api.sit.block.BlockSitStrategy
import plutoproject.feature.paper.api.sit.block.events.PlayerSitOnBlockEvent
import plutoproject.feature.paper.api.sit.block.events.PlayerStandUpFromBlockEvent
import plutoproject.feature.paper.sit.block.strategies.*
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.world.subtract
import kotlin.math.max
import kotlin.reflect.KClass

class BlockSitImpl : BlockSit {
    override val allStrategies: Collection<BlockSitStrategy>
        get() = strategies.keys
    override val sitters: Collection<Player>
        get() = sitContexts.keys

    private val seatEntityMarkerKey = NamespacedKey(plugin, "sit.block_sit_marker")
    private val sitContexts = mutableMapOf<Player, BlockSitContext>()
    private val strategies = mutableMapOf(
        SlabBlockSitStrategy to Int.MAX_VALUE - 1,
        StairBlockSitStrategy to Int.MAX_VALUE - 1,
        CarpetBlockSitStrategy to Int.MAX_VALUE - 1,
        CampfireBlockSitStrategy to Int.MAX_VALUE - 1,
        ScaffoldingBlockSitStrategy to Int.MAX_VALUE - 1,
        DefaultBlockSitStrategy to Int.MAX_VALUE,
    )

    override fun isSitting(player: Player): Boolean {
        return sitContexts[player] != null
    }

    private fun createSeatEntity(location: Location): ArmorStand {
        return location.world.spawn(location, ArmorStand::class.java).apply {
            setGravity(false)
            setAI(false)
            setCanTick(false)
            setCanMove(false)
            isInvulnerable = true
            isInvisible = true
            isCollidable = false
            persistentDataContainer.set(seatEntityMarkerKey, PersistentDataType.BOOLEAN, true)
        }
    }

    private fun removeSeatEntity(sitter: Player) {
        val seatEntity = sitContexts[sitter]?.seatEntity!!
        seatEntity.removePassenger(sitter)
        seatEntity.remove()
    }

    override fun getSeat(player: Player): Block? {
        if (!isSitting(player)) {
            return null
        }
        return sitContexts[player]!!.block
    }

    override fun getSitter(block: Block): Player? {
        return sitContexts.entries.firstOrNull { it.value.block == block }?.key
    }

    override fun getSitter(blockLocation: Location): Player? {
        return getSitter(blockLocation.block)
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
            getSeat(sitter)!!
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

    private fun BoundingBox.convertRelativeToAbsolute(base: Vector): BoundingBox {
        return BoundingBox(
            minX + base.x,
            minY + base.y,
            minZ + base.z,
            maxX + base.x,
            maxY + base.y,
            maxZ + base.z
        )
    }

    override fun sit(sitter: Player, target: Block, sitOptions: SitOptions): SitFinalResult {
        check(Bukkit.isPrimaryThread()) { "Sit operation can only be performed on main thread." }

        if (isSitting(sitter)) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_ALREADY_SITTING, target, null)
            return SitFinalResult.FAILED_ALREADY_SITTING
        }
        if (sitContexts.values.any { it.block == target }) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_TARGET_OCCUPIED, target, null)
            return SitFinalResult.FAILED_TARGET_OCCUPIED
        }

        val strategy = getStrategy(target)

        if (strategy == null || !strategy.isAllowed(target)) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_INVALID_TARGET, target, null)
            return SitFinalResult.FAILED_INVALID_TARGET
        }

        val sitLocation = strategy.getSitLocation(sitter, target)
        val sitDirection = strategy.getSitDirection(sitter, target)

        val minX = target.location.x
        val minY = sitLocation.y
        val minZ = target.location.z
        val maxX = minX + 1
        val maxY = minY + 1.5
        val maxZ = minZ + 1

        val corner1 = Vector(minX, minY, minZ)
        val corner2 = Vector(maxX, maxY, maxZ)
        val sitCollisionShape = BoundingBox.of(corner1, corner2)

        val blockCollisionShape = target.collisionShape.boundingBoxes.map {
            // 这玩意返回的 BoundingBox 里怎么是相对位置啊？？
            it.convertRelativeToAbsolute(target.location.toVector())
        }
        val selfExcluded = sitCollisionShape.subtract(blockCollisionShape)

        if (selfExcluded.any { target.world.hasCollisionsIn(it) }) {
            callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.FAILED_TARGET_BLOCKED_BY_BLOCKS, target, null)
            return SitFinalResult.FAILED_TARGET_BLOCKED_BY_BLOCKS
        }

        val event = callSitOnBlockEvent(sitter, sitOptions, SitAttemptResult.SUCCEED, target, strategy)

        if (event.isCancelled) {
            return SitFinalResult.FAILED_CANCELLED_BY_PLUGIN
        }

        val seatEntityLocation = sitLocation.clone().apply {
            subtract(0.0, 2.0, 0.0)
            yaw = sitDirection.yaw
        }
        val seatEntity = createSeatEntity(seatEntityLocation)

        if (sitOptions.playSitSound) {
            sitter.playSitSound()
        }

        sitContexts[sitter] = BlockSitContext(target, seatEntity, sitOptions)
        seatEntity.addPassenger(sitter)

        return SitFinalResult.SUCCEED
    }

    override fun sit(sitter: Player, target: Location, sitOptions: SitOptions): SitFinalResult {
        return sit(sitter, target.block, sitOptions)
    }

    override fun standUp(sitter: Player): Boolean {
        check(Bukkit.isPrimaryThread()) { "Stand up operation can only be performed on main thread." }

        if (!isSitting(sitter)) {
            return false
        }

        val sitContext = sitContexts[sitter]
        val standUpLocation = sitter.location.clone().apply {
            // 某些方块 (MOVING_PISTON) 的顶面高度返回 0
            val maxY = max(sitContext!!.block.boundingBox.maxY, sitContext.block.location.y)
            y = maxY + 0.5
        }

        if (callPlayerStandUpFromBlockEvent(sitter).isCancelled) {
            return false
        }

        if (getOptions(sitter)!!.playSitSound) {
            sitter.playSitSound()
        }

        removeSeatEntity(sitter)
        sitter.teleport(standUpLocation)
        sitContexts.remove(sitter)
        sitter.sendActionBar(Component.empty())

        return true
    }

    override fun isTemporarySeatEntity(entity: Entity): Boolean {
        return entity.persistentDataContainer.has(seatEntityMarkerKey)
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
