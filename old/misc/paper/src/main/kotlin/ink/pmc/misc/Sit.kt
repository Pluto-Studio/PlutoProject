package ink.pmc.misc

import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.entity.ensureThreadSafe
import ink.pmc.framework.world.ensureThreadSafe
import ink.pmc.framework.world.eraseAngle
import ink.pmc.misc.api.sit.*
import kotlinx.coroutines.delay
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Hangable
import org.bukkit.block.data.Openable
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.type.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.time.Duration.Companion.seconds

/*
* 有时候在尝试坐下的时候会由于未知原因直接触发 EntityDismountEvent，导致无法坐下。
* 猜测可能是和碰撞有关。设置一个延迟机制，来解决这个问题。
* */
val sitDelay = CopyOnWriteArraySet<UUID>()
val armorStandDataKey = NamespacedKey(plugin, "sit-passenger")

fun runSitCheckTask() {
    submitAsync {
        while (!disabled) {
            SitManager.sitters.forEach {
                val player = it

                player.ensureThreadSafe {
                    val armorStand = player.seat ?: return@ensureThreadSafe // 有时可能玩家已经站起来了，但是异步任务仍然尝试获取实体

                    // 避免一些意外问题
                    if (!armorStand.passengers.contains(player)) {
                        player.stand()
                    }
                }
            }

            plugin.server.onlinePlayers.forEach {
                val chunk = it.chunk

                chunk.ensureThreadSafe {
                    clearIllegalArmorStands(this)
                }
            }

            delay(2.seconds)
        }
    }
}

fun runActionBarOverrideTask() {
    submitAsync {
        while (!disabled) {
            SitManager.sitters.forEach {
                it.sendActionBar(STAND_UP)
            }
            delay(5)
        }
    }
}

fun clearIllegalArmorStands(chunk: Chunk) {
    val entities = chunk.entities
    val armorStands = entities.filter { it.persistentDataContainer.has(armorStandDataKey) }

    armorStands.forEach {
        if (getSitter(it) != null && getSitter(it)?.seat != it) {
            it.remove()
            val location = it.location
            plugin.logger.info("Removed illegal armor stand at ${location.x}, ${location.y}, ${location.z}, ${location.world.name}.")
        }
    }
}

fun createArmorStand(locationToSit: Location): Entity {
    /*
    * TODO: 替换为能在 Folia 上使用的 API
    * 由于 Folia 目前似乎暂无完整的世界操作 API，所以先搁置
    * */
    val armorStandLoc = offsetLocation(locationToSit)

    val world = armorStandLoc.world
    val entity = world.spawn(armorStandLoc, ArmorStand::class.java)

    entity.setGravity(false)
    entity.isInvisible = true
    entity.setAI(false)
    entity.isCollidable = false
    entity.setCanTick(false)

    return entity
}

fun markDelay(player: Player) {
    sitDelay.add(player.uniqueId)
    submitAsync {
        delay(100)
        sitDelay.remove(player.uniqueId)
    }
}

fun tryToStand(event: EntityEvent, needCancel: Boolean = true) {
    val entity = event.entity

    if (entity !is Player) {
        return
    }

    if (!entity.isSitting) {
        return
    }

    if (event is Cancellable) {
        event.isCancelled = needCancel
    }

    entity.stand()
}

fun handleSitClick(event: PlayerInteractEvent) {
    val player = event.player

    if (event.hand != EquipmentSlot.HAND) {
        return
    }

    if (!event.action.isRightClick) {
        return
    }

    if (player.inventory.itemInMainHand.type != Material.AIR) {
        return
    }

    if (event.clickedBlock == null) {
        return
    }

    val block = event.clickedBlock!!

    if (player.isSitting) {
        return
    }

    val blockData = block.blockData

    if (blockData !is Slab && blockData !is Stairs && !isExtinctCampfire(block)) {
        return
    }

    val location = block.location

    if (!checkLocation(location)) {
        return
    }

    if (player.location.blockY + 1 < location.y) {
        return
    }

    event.isCancelled = true
    player.swingMainHand()
    player.sit(location)
}

fun handlePlayerQuit(event: PlayerQuitEvent) {
    val player = event.player

    if (!player.isSitting) {
        return
    }

    player.stand()
    sitDelay.remove(player.uniqueId)
}

fun markAsSeat(entity: Entity, sitter: Player) {
    entity.persistentDataContainer.set(armorStandDataKey, PersistentDataType.STRING, sitter.uniqueId.toString())
}

fun isSeat(entity: Entity): Boolean {
    return entity.persistentDataContainer.has(armorStandDataKey)
}

fun getSitter(entity: Entity): Player? {
    val uuid = UUID.fromString(entity.persistentDataContainer.get(armorStandDataKey, PersistentDataType.STRING))
    return plugin.server.getPlayer(uuid)
}

fun handleSitDelay(event: EntityDismountEvent) {
    if (!sitDelay.contains(event.entity.uniqueId)) {
        tryToStand(event, false)
    } else {
        event.isCancelled = true
    }
}

fun handleSitLocationBroke(event: Event) {
    val locations = mutableSetOf<Location>()

    when (event) {
        is BlockEvent -> {
            locations.add(event.block.location.eraseAngle())
            when (event) {
                is BlockPistonExtendEvent -> event.blocks.map { it.location.eraseAngle() }.toCollection(locations)
                is BlockPistonRetractEvent -> event.blocks.map { it.location.eraseAngle() }.toCollection(locations)
                is BlockExplodeEvent -> event.blockList().map { it.location.eraseAngle() }.toCollection(locations)
            }
        }

        is EntityExplodeEvent -> {
            event.blockList().map { it.location.eraseAngle() }.toCollection(locations)
        }

        is EntitySpawnEvent -> {
            if (event.entity.type == EntityType.FALLING_BLOCK) {
                val location = event.location.eraseAngle()
                if (location.eraseAngle().isSitLocation) {
                    locations.add(location)
                }
            }
        }
    }

    locations.forEach {
        if (!it.isSitLocation) {
            return@forEach
        }

        val player = it.sitter!!
        player.stand()
    }
}

fun handleArmorStandAction(event: Cancellable, needCancel: Boolean = true) {
    if (event !is EntityEvent) {
        return
    }

    val entity = event.entity

    if (entity !is ArmorStand) {
        return
    }

    if (!isSeat(entity) || getSitter(entity) == null) {
        return
    }

    event.isCancelled = needCancel
}

private fun isSlab(block: Block): Boolean {
    return block.blockData is Slab
}

private fun isStair(block: Block): Boolean {
    return block.blockData is Stairs
}

private fun treatAsNormal(block: Block): Boolean {
    val blockData = block.blockData

    if (isSlab(block)) {
        val slab = blockData as Slab
        return slab.type == Slab.Type.TOP || slab.type == Slab.Type.DOUBLE
    }

    if (isStair(block)) {
        val stair = blockData as Stairs
        return stair.half == Bisected.Half.TOP
    }

    if (isExtinctCampfire(block)) {
        return false
    }

    return true
}

private fun offsetLocation(location: Location): Location {
    val block = location.block
    val loc = location.clone()

    loc.add(0.5, 0.0, 0.5)
    loc.subtract(0.0, 1.0, 0.0)

    if (!treatAsNormal(block)) {
        loc.subtract(0.0, 0.5, 0.0)
    }

    if (isStair(block)) {
        return if (treatAsNormal(block)) loc else offsetStair(block, loc)
    }

    if (isExtinctCampfire(block)) {
        return offsetCampfire(loc)
    }

    if (isBlossom(block)) {
        loc.subtract(0.0, 1.0, 0.0)
    }

    return loc
}

private fun offsetCampfire(loc: Location): Location {
    return loc.clone().subtract(0.0, 0.05, 0.0)
}

private fun offsetStair(block: Block, location: Location): Location {
    val loc = location.clone()
    val blockData = block.blockData as Stairs

    val facing = blockData.facing
    val shape = blockData.shape

    when (facing) {
        BlockFace.NORTH -> loc.add(0.0, 0.0, 0.25)
        BlockFace.SOUTH -> loc.subtract(0.0, 0.0, 0.25)
        BlockFace.WEST -> loc.add(0.25, 0.0, 0.0)
        BlockFace.EAST -> loc.subtract(0.25, 0.0, 0.0)
        else -> {}
    }

    return offsetStairShape(facing, shape, loc)
}

private fun offsetStairShape(facing: BlockFace, shape: Stairs.Shape, location: Location): Location {
    val loc = location.clone()

    when (facing) {
        BlockFace.NORTH -> loc.apply {
            if (shape.name.endsWith("LEFT")) {
                add(0.25, 0.0, 0.0)
            } else if (shape.name.endsWith("RIGHT")) {
                subtract(0.25, 0.0, 0.0)
            }
        }

        BlockFace.SOUTH -> loc.apply {
            if (shape.name.endsWith("LEFT")) {
                subtract(0.25, 0.0, 0.0)
            } else if (shape.name.endsWith("RIGHT")) {
                add(0.25, 0.0, 0.0)
            }
        }

        BlockFace.WEST -> loc.apply {
            if (shape.name.endsWith("LEFT")) {
                subtract(0.0, 0.0, 0.25)
            } else if (shape.name.endsWith("RIGHT")) {
                add(0.0, 0.0, 0.25)
            }
        }

        BlockFace.EAST -> loc.apply {
            if (shape.name.endsWith("LEFT")) {
                add(0.0, 0.0, 0.25)
            } else if (shape.name.endsWith("RIGHT")) {
                subtract(0.0, 0.0, 0.25)
            }
        }

        else -> {}
    }

    return loc
}

fun isExtinctCampfire(block: Block): Boolean {
    val data = block.blockData

    if (data is Campfire) {
        if (!data.isLit) {
            return true
        }
    }

    return false
}

fun isBlossom(block: Block): Boolean {
    val data = block.blockData
    val type = block.type

    if (data is PinkPetals) {
        return true
    }

    if (type == Material.SHORT_GRASS || type == Material.TALL_GRASS || type == Material.SEAGRASS || type == Material.VINE) {
        return true
    }

    return false
}

private fun isAzalea(block: Block): Boolean {
    val type = block.type
    return type == Material.AZALEA || type == Material.FLOWERING_AZALEA
}

private fun isLegalType(block: Block): Boolean {
    val blockData = block.blockData

    if (isExtinctCampfire(block)) {
        return true
    }

    if (isBlossom(block)) {
        return true
    }

    if (isAzalea(block)) {
        return true
    }

    if (!(block.isSolid || isSlab(block) || isStair(block))) {
        return false
    }

    if (blockData is Sign) {
        return false
    }

    if (blockData is Openable || blockData is Hangable || blockData is Powerable) {
        return false
    }

    return true
}

fun checkLocation(location: Location): Boolean {
    val block = location.block

    if (!isLegalType(block)) {
        return false
    }

    val offset1 = location.clone().add(0.0, 1.0, 0.0).block
    val offset2 = location.clone().add(0.0, 2.0, 0.0).block

    return (offset1.type == Material.AIR && offset2.type == Material.AIR)
            || (isBlossom(block) && (isBlossom(offset1)
            || offset1.type == Material.AIR) && (isBlossom(offset2)
            || offset2.type == Material.AIR))
}

fun findLegalLocation(startPoint: Location): Location? {
    for (i in 1..10) {
        val location = startPoint.clone().subtract(0.0, i.toDouble(), 0.0)

        if (!checkLocation(location)) {

            /*
            * 可能存在找到实心位置，但是没有空间的情况。
            * 在这种情况下直接判定为不允许坐下，并且不继续寻找下一层以避免穿透的问题。
            * */
            if (location.block.isSolid) {
                return null
            }

            continue
        }

        return location.toBlockLocation()
    }

    return null
}