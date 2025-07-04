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
import plutoproject.feature.paper.api.sit.BlockSitAttemptResult
import plutoproject.feature.paper.api.sit.BlockSitFinalResult
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.block.BlockSitStrategy
import plutoproject.feature.paper.api.sit.block.SitOnBlockCause
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause
import plutoproject.feature.paper.api.sit.block.events.PlayerSitOnBlockEvent
import plutoproject.feature.paper.api.sit.block.events.PlayerStandUpFromBlockEvent
import plutoproject.feature.paper.sit.block.strategies.*
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.world.subtract
import kotlin.math.max
import kotlin.reflect.KClass

class BlockSitImpl : InternalBlockSit {
    override val allStrategies: Collection<BlockSitStrategy>
        get() = strategies.keys
    override val sitters: Collection<Player>
        get() = sitContexts.keys

    private val seatEntityMarkerKey = NamespacedKey(plugin, "sit.block_sit_marker")
    override val sitContexts = mutableMapOf<Player, BlockSitContext>()

    override fun isSeatEntityInUse(entity: ArmorStand): Boolean {
        if (!isTemporarySeatEntity(entity)) return false
        return sitContexts.values.any { it.seatEntity == entity }
    }

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

    override fun getSeat(player: Player): Block? {
        if (!isSitting(player)) {
            return null
        }
        return sitContexts[player]!!.block
    }

    override fun getSitter(seat: Block): Player? {
        return sitContexts.entries.firstOrNull { it.value.block == seat }?.key
    }

    override fun getSitter(seat: Location): Player? {
        return getSitter(seat.block)
    }

    override fun getOptions(player: Player): SitOptions? {
        return sitContexts[player]?.options
    }

    private fun callSitOnBlockEvent(
        sitter: Player,
        options: SitOptions,
        cause: SitOnBlockCause,
        attemptResult: BlockSitAttemptResult,
        seat: Block,
        strategy: BlockSitStrategy?
    ): PlayerSitOnBlockEvent {
        return PlayerSitOnBlockEvent(sitter, options, cause, attemptResult, seat, strategy).apply { callEvent() }
    }

    private fun callPlayerStandUpFromBlockEvent(
        sitter: Player,
        cause: StandUpFromBlockCause
    ): PlayerStandUpFromBlockEvent {
        return PlayerStandUpFromBlockEvent(
            sitter,
            getOptions(sitter)!!,
            cause,
            getSeat(sitter)!!,
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

    override fun sit(
        player: Player,
        target: Block,
        sitOptions: SitOptions,
        cause: SitOnBlockCause
    ): BlockSitFinalResult {
        check(Bukkit.isPrimaryThread()) { "Sit operation can only be performed on main thread." }

        if (isSitting(player)) {
            val event = callSitOnBlockEvent(
                player, sitOptions, cause, BlockSitAttemptResult.ALREADY_SITTING, target, null
            )
            return if (event.isCancelled) BlockSitFinalResult.CANCELLED_BY_PLUGIN else BlockSitFinalResult.ALREADY_SITTING
        }
        if (sitContexts.values.any { it.block == target }) {
            val event = callSitOnBlockEvent(
                player, sitOptions, cause, BlockSitAttemptResult.SEAT_OCCUPIED, target, null
            )
            return if (event.isCancelled) BlockSitFinalResult.CANCELLED_BY_PLUGIN else BlockSitFinalResult.SEAT_OCCUPIED
        }

        val strategy = getStrategy(target)

        if (strategy == null || !strategy.isAllowed(target)) {
            val event = callSitOnBlockEvent(
                player, sitOptions, cause, BlockSitAttemptResult.INVALID_SEAT, target, null
            )
            return if (event.isCancelled) BlockSitFinalResult.CANCELLED_BY_PLUGIN else BlockSitFinalResult.INVALID_SEAT
        }

        val sitLocation = strategy.getSitLocation(player, target)
        val sitDirection = strategy.getSitDirection(player, target)

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
            val event = callSitOnBlockEvent(
                player, sitOptions, cause, BlockSitAttemptResult.BLOCKED_BY_BLOCKS, target, null
            )
            return if (event.isCancelled) BlockSitFinalResult.CANCELLED_BY_PLUGIN else BlockSitFinalResult.BLOCKED_BY_BLOCKS
        }

        val event = callSitOnBlockEvent(player, sitOptions, cause, BlockSitAttemptResult.SUCCEED, target, strategy)

        if (event.isCancelled) {
            return BlockSitFinalResult.CANCELLED_BY_PLUGIN
        }

        val seatEntityLocation = sitLocation.clone().apply {
            subtract(0.0, 2.0, 0.0)
            yaw = sitDirection.yaw
        }
        val seatEntity = createSeatEntity(seatEntityLocation)

        if (sitOptions.playSitSound) {
            player.playSitSound()
        }

        sitContexts[player] = BlockSitContext(target, seatEntity, sitOptions)
        seatEntity.addPassenger(player)

        return BlockSitFinalResult.SUCCEED
    }

    override fun sit(
        player: Player,
        target: Location,
        sitOptions: SitOptions,
        cause: SitOnBlockCause
    ): BlockSitFinalResult {
        return sit(player, target.block, sitOptions, cause)
    }

    private val ignoreCancelStandUpCauses = arrayOf(
        StandUpFromBlockCause.QUIT,
        StandUpFromBlockCause.FEATURE_DISABLE,
    )

    override fun standUp(player: Player, cause: StandUpFromBlockCause): Boolean {
        check(Bukkit.isPrimaryThread()) { "Stand up operation can only be performed on main thread." }

        if (!isSitting(player)) {
            return false
        }

        val sitContext = sitContexts[player]!!
        val standUpLocation = player.location.clone().apply {
            // 某些方块 (MOVING_PISTON) 的顶面高度返回 0
            val maxY = max(sitContext.block.boundingBox.maxY, sitContext.block.location.y)
            y = maxY + 0.5
        }

        if (callPlayerStandUpFromBlockEvent(player, cause).isCancelled && !ignoreCancelStandUpCauses.contains(cause)) {
            return false
        }

        if (getOptions(player)!!.playSitSound) {
            player.playSitSound()
        }

        sitContexts.remove(player)
        sitContext.seatEntity.remove()
        player.teleport(standUpLocation)
        player.sendActionBar(Component.empty())

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
