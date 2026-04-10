package plutoproject.feature.gallery.adapter.paper.listener

import ink.pmc.advkt.component.text
import ink.pmc.advkt.playSound
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.sound.*
import ink.pmc.advkt.title.*
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.MapId
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction.*
import kotlinx.coroutines.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.Rotation
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.inventory.ItemStack
import plutoproject.feature.gallery.adapter.common.DisplayInstanceIndex
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.adapter.paper.*
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.framework.common.util.chat.component.replace
import java.util.*
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds

private const val IMAGE_LOOKUP_TIMEOUT_SECONDS = 5
private const val DISPLAY_INSTANCE_SAVE_TIMEOUT_SECONDS = 10

private val PLACEMENT_FAILED_SOUND = sound {
    key(Key.key("block.note_block.hat"))
    source(Sound.Source.BLOCK)
    volume(1f)
    pitch(1f)
}

@Suppress("UNUSED", "UnstableApiUsage")
object ItemFrameListener : Listener {
    private val logger = koin.get<Logger>()
    private val imageStore = koin.get<ImageStore>()
    private val imageDataStore = koin.get<ImageDataStore>()
    private val displayInstanceStore = koin.get<DisplayInstanceStore>()
    private val displayRuntime = koin.get<DisplayRuntimeRegistry>()
    private val displayIndex = koin.get<DisplayInstanceIndex>()
    private val coroutineScope = koin.get<CoroutineScope>()

    @EventHandler
    suspend fun onItemFrameChange(event: PlayerItemFrameChangeEvent) {
        when (event.action) {
            PLACE -> onPlace(event)
            REMOVE -> onRemove(event)
            ROTATE -> return
        }
    }

    private suspend fun onPlace(event: PlayerItemFrameChangeEvent) {
        val player = event.player
        val itemFrame = event.itemFrame
        val itemStack = event.itemStack

        if (event.action != PLACE || itemStack.type != IMAGE_ITEM_MATERIAL) {
            return
        }

        val imageItemData = itemStack.imageItemData() ?: return
        val imageDeferred = coroutineScope.async(Dispatchers.IO) {
            withTimeoutOrNull(IMAGE_LOOKUP_TIMEOUT_SECONDS.seconds) {
                imageStore.get(imageItemData.imageId)
            }
        }
        val imageDataDeferred = coroutineScope.async(Dispatchers.IO) {
            withTimeoutOrNull(IMAGE_LOOKUP_TIMEOUT_SECONDS.seconds) {
                imageDataStore.get(imageItemData.imageId)
            }
        }

        val width = imageItemData.widthBlocks
        val height = imageItemData.heightBlocks

        val playerHorizontalFacing = normalizeHorizontalFacing(player.facing)
        val axis = wallAxisOf(itemFrame.facing, playerHorizontalFacing)
        val frameRotation = frameRotationOf(itemFrame.facing, playerHorizontalFacing)

        val connectedFrames = collectConnectedFrames(itemFrame, axis)
        val framesByGridPos: Map<GridPos, ItemFrame> = connectedFrames.associateBy { frame ->
            toGridPos(itemFrame, frame, axis)
        }

        val placement = findPlacement(framesByGridPos, width, height)

        if (placement == null) {
            event.isCancelled = true

            imageDeferred.cancel()
            imageDataDeferred.cancel()

            player.playSound(PLACEMENT_FAILED_SOUND)
            player.showTitle {
                mainTitle {
                    text(" ")
                }
                subTitle(
                    IMAGE_ITEM_PLACEMENT_FAILED_NO_SPACE_SUBTITLE
                        .replace("<width>", imageItemData.widthBlocks)
                        .replace("<height>", imageItemData.heightBlocks)
                )
                times {
                    fadeIn(0.seconds)
                    stay(1.seconds)
                    fadeOut(0.seconds)
                }
            }
            return
        }

        val selectedFrames = buildList {
            for (tileY in 0 until height) {
                for (tileX in 0 until width) {
                    val gridPos = GridPos(placement.left + tileX, placement.top + tileY)
                    add(PlacedItemFrame(tileX, tileY, framesByGridPos[gridPos] ?: error("Unexpected")))
                }
            }
        }

        check(selectedFrames.isNotEmpty()) { "Unexpected, selectedFrames should not be empty" }

        val origin = selectedFrames.first().itemFrame
        val originLocation = origin.location
        val displayInstance = DisplayInstance(
            id = UUID.randomUUID(),
            imageId = imageItemData.imageId,
            world = itemFrame.world.name,
            chunkX = origin.chunk.x,
            chunkZ = origin.chunk.z,
            facing = origin.facing.itemFrameFacing(),
            widthBlocks = imageItemData.widthBlocks,
            heightBlocks = imageItemData.heightBlocks,
            originX = originLocation.x,
            originY = originLocation.y,
            originZ = originLocation.z,
            itemFrameIds = selectedFrames.map { it.itemFrame.uniqueId }
        )

        selectedFrames.forEachIndexed { index, frame ->
            val tileIndex = frame.tileY * width + frame.tileX
            val mapId = imageItemData.tileMapIds[tileIndex]
            val nextFrame = selectedFrames.getOrNull(index + 1)
            val itemStack = ItemStack(Material.FILLED_MAP).apply {
                setData(DataComponentTypes.MAP_ID, MapId.mapId(mapId))
            }
            if (frame.itemFrame == itemFrame) {
                event.setItemStack(itemStack)
            } else {
                frame.itemFrame.setItem(itemStack)
            }
            frame.itemFrame.rotation = frameRotation
            frame.itemFrame.setImageItemFrame(displayInstance, nextFrame?.itemFrame?.uniqueId)
        }

        displayIndex.add(
            itemFrame.world.name,
            ChunkKey(origin.chunk.x, origin.chunk.z),
            displayInstance.id
        )

        val image = imageDeferred.await()
        val imageData = imageDataDeferred.await()

        if (image == null || imageData == null) {
            if (image != null) {
                logger.warning("Cannot find image data for existed image \"${image.name}\" (${image.id})")
            }
            player.playSound(PLACEMENT_FAILED_SOUND)
            player.sendMessage(IMAGE_ITEM_PLACEMENT_FAILED_INVALID)
            rollbackPlacement(selectedFrames, displayInstance)
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            withTimeout(DISPLAY_INSTANCE_SAVE_TIMEOUT_SECONDS.seconds) {
                displayInstanceStore.create(displayInstance)
            }
        }

        displayRuntime.attach(image, imageData, displayInstance)
    }

    private suspend fun rollbackPlacement(itemFrames: List<PlacedItemFrame>, displayInstance: DisplayInstance) {
        val originFrame = itemFrames.first().itemFrame
        itemFrames.map { it.itemFrame }.forEach { itemFrame ->
            itemFrame.setItem(ItemStack.empty())
            itemFrame.unsetImageItemFrame()
        }
        displayIndex.remove(
            originFrame.world.name,
            ChunkKey(originFrame.chunk.x, originFrame.chunk.z),
            displayInstance.id
        )
    }

    private suspend fun onRemove(event: PlayerItemFrameChangeEvent) {
        val itemFrame = event.itemFrame
        val itemStack = event.itemStack

        if (event.action != REMOVE || itemStack.type != Material.FILLED_MAP) {
            return
        }

        val imageItemFrameData = itemFrame.imageItemFrameData() ?: return
        val displayInstance = displayRuntime.detach(imageItemFrameData.imageId, imageItemFrameData.displayInstanceId)

        val originFrame = event.itemFrame.world.getEntity(imageItemFrameData.originItemFrame)
        val imageDeferred = coroutineScope.async(Dispatchers.IO) {
            withTimeoutOrNull(IMAGE_LOOKUP_TIMEOUT_SECONDS.seconds) {
                imageStore.get(imageItemFrameData.imageId)
            }
        }

        event.setItemStack(ItemStack.empty())

        if (originFrame !is ItemFrame) {
            val imageItem = removeWithDisplayInstance(itemFrame, imageDeferred, displayInstance) ?: return
            itemFrame.world.dropItemNaturally(itemFrame.location, imageItem)
            return
        }

        deleteDisplayInstance(imageItemFrameData.displayInstanceId)

        displayIndex.remove(
            itemFrame.world.name,
            ChunkKey(originFrame.chunk.x, originFrame.chunk.z),
            imageItemFrameData.displayInstanceId
        )

        clearDisplayFrames(originFrame, itemFrame.world)

        val image = imageDeferred.await()
        val imageItem = image?.let { createImageItem(it) } ?: ItemStack.empty()

        // TODO: 模拟真实物品展示框掉落
        itemFrame.world.dropItemNaturally(itemFrame.location, imageItem)
    }

    private suspend fun removeWithDisplayInstance(
        itemFrame: ItemFrame,
        imageDeferred: Deferred<Image?>,
        displayInstance: DisplayInstance?
    ): ItemStack? {
        val imageItemFrameData = itemFrame.imageItemFrameData() ?: return null
        val displayInstance = resolveDisplayInstanceForRemoval(imageItemFrameData.displayInstanceId, displayInstance)
        if (displayInstance == null) {
            imageDeferred.cancel()
            return null
        }

        displayIndex.remove(
            itemFrame.world.name,
            ChunkKey(displayInstance.chunkX, displayInstance.chunkZ),
            displayInstance.id
        )

        clearDisplayFrames(displayInstance, itemFrame.world)

        val image = imageDeferred.await()
        return image?.let { createImageItem(it) } ?: ItemStack.empty()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    suspend fun onItemFrameBreak(event: HangingBreakEvent) {
        if (event.cause == HangingBreakEvent.RemoveCause.ENTITY) {
            return
        }

        val itemFrame = event.entity as? ItemFrame ?: return

        val imageItemFrameData = itemFrame.imageItemFrameData() ?: return
        val displayInstance = displayRuntime.detach(imageItemFrameData.imageId, imageItemFrameData.displayInstanceId)

        val originFrame = itemFrame.world.getEntity(imageItemFrameData.originItemFrame)
        val imageDeferred = coroutineScope.async(Dispatchers.IO) {
            withTimeoutOrNull(IMAGE_LOOKUP_TIMEOUT_SECONDS.seconds) {
                imageStore.get(imageItemFrameData.imageId)
            }
        }

        event.isCancelled = true

        if (originFrame !is ItemFrame) {
            val imageItem = removeWithDisplayInstance(itemFrame, imageDeferred, displayInstance) ?: return
            val itemFrameItem = createItemFrameItem(itemFrame)

            itemFrame.remove()
            playBreakSound(itemFrame)

            // TODO: 模拟真实物品展示框掉落
            if (event.cause != HangingBreakEvent.RemoveCause.EXPLOSION) {
                itemFrame.world.dropItemNaturally(itemFrame.location, itemFrameItem)
                itemFrame.world.dropItemNaturally(itemFrame.location, imageItem)
            }
            return
        }

        deleteDisplayInstance(imageItemFrameData.displayInstanceId)

        displayIndex.remove(
            itemFrame.world.name,
            ChunkKey(originFrame.chunk.x, originFrame.chunk.z),
            imageItemFrameData.displayInstanceId
        )

        clearDisplayFrames(originFrame, itemFrame.world)

        val image = imageDeferred.await()
        val imageItem = image?.let { createImageItem(it) } ?: ItemStack.empty()
        val itemFrameItem = createItemFrameItem(itemFrame)

        itemFrame.remove()
        playBreakSound(itemFrame)

        // TODO: 模拟真实物品展示框掉落
        if (event.cause != HangingBreakEvent.RemoveCause.EXPLOSION) {
            itemFrame.world.dropItemNaturally(itemFrame.location, itemFrameItem)
            itemFrame.world.dropItemNaturally(itemFrame.location, imageItem)
        }
    }

    private fun deleteDisplayInstance(displayInstanceId: UUID) {
        coroutineScope.launch(Dispatchers.IO) {
            displayInstanceStore.delete(displayInstanceId)
        }
    }

    private suspend fun resolveDisplayInstanceForRemoval(
        displayInstanceId: UUID,
        displayInstance: DisplayInstance?
    ): DisplayInstance? {
        val displayInstanceDeferred = coroutineScope.async(Dispatchers.IO) {
            withTimeoutOrNull(IMAGE_LOOKUP_TIMEOUT_SECONDS.seconds) {
                displayInstanceStore.delete(displayInstanceId)
            }
        }

        return displayInstance ?: displayInstanceDeferred.await()
    }

    private fun clearDisplayFrames(displayInstance: DisplayInstance, world: org.bukkit.World) {
        displayInstance.itemFrameIds
            .mapNotNull { world.getEntity(it) }
            .filterIsInstance<ItemFrame>()
            .forEach(::clearDisplayFrame)
    }

    private fun clearDisplayFrames(originFrame: ItemFrame, world: org.bukkit.World) {
        var currentFrame: ItemFrame? = originFrame

        while (currentFrame != null) {
            val frameData = currentFrame.imageItemFrameData()
            clearDisplayFrame(currentFrame)
            currentFrame = frameData?.nextItemFrame?.let { world.getEntity(it) as? ItemFrame }
        }
    }

    private fun clearDisplayFrame(itemFrame: ItemFrame) {
        itemFrame.unsetImageItemFrame()
        itemFrame.rotation = Rotation.NONE
        itemFrame.setItem(ItemStack.empty())
    }

    private fun createItemFrameItem(itemFrame: ItemFrame): ItemStack {
        return when (itemFrame.type) {
            EntityType.ITEM_FRAME -> ItemStack(Material.ITEM_FRAME)
            EntityType.GLOW_ITEM_FRAME -> ItemStack(Material.GLOW_ITEM_FRAME)
            else -> error("Unexpected")
        }
    }

    private fun playBreakSound(itemFrame: ItemFrame) {
        itemFrame.world.playSound(itemFrame.x, itemFrame.y, itemFrame.z) {
            val soundKey = when (itemFrame.type) {
                EntityType.ITEM_FRAME -> "entity.item_frame.break"
                EntityType.GLOW_ITEM_FRAME -> "entity.glow_item_frame.break"
                else -> error("Unexpected")
            }
            key(Key.key(soundKey))
            source(Sound.Source.NEUTRAL)
        }
    }
}

internal data class WallAxis(
    val right: BlockFace,
    val down: BlockFace,
)

private data class GridPos(
    val u: Int,
    val v: Int,
)

private data class Placement(
    val left: Int,
    val top: Int,
)

private data class PlacedItemFrame(
    val tileX: Int,
    val tileY: Int,
    val itemFrame: ItemFrame,
)

private fun BlockFace.itemFrameFacing(): ItemFrameFacing {
    return when (this) {
        BlockFace.NORTH -> ItemFrameFacing.NORTH
        BlockFace.EAST -> ItemFrameFacing.EAST
        BlockFace.SOUTH -> ItemFrameFacing.SOUTH
        BlockFace.WEST -> ItemFrameFacing.WEST
        BlockFace.UP -> ItemFrameFacing.UP
        BlockFace.DOWN -> ItemFrameFacing.DOWN
        else -> error("Unexpected")
    }
}

private fun normalizeHorizontalFacing(facing: BlockFace): BlockFace {
    val x = facing.modX
    val z = facing.modZ

    require(x != 0 || z != 0) { "Expected horizontal facing, got $facing" }

    return if (kotlin.math.abs(x) >= kotlin.math.abs(z)) {
        if (x >= 0) BlockFace.EAST else BlockFace.WEST
    } else {
        if (z >= 0) BlockFace.SOUTH else BlockFace.NORTH
    }
}

private fun rightOf(facing: BlockFace): BlockFace = when (facing) {
    BlockFace.NORTH -> BlockFace.EAST
    BlockFace.EAST -> BlockFace.SOUTH
    BlockFace.SOUTH -> BlockFace.WEST
    BlockFace.WEST -> BlockFace.NORTH
    else -> error("Expected 4-way horizontal facing, got $facing")
}

internal fun frameRotationOf(
    frameFacing: BlockFace,
    playerHorizontalFacing: BlockFace,
): Rotation {
    return when (frameFacing) {
        BlockFace.UP -> floorFrameRotationOf(playerHorizontalFacing)
        BlockFace.DOWN -> ceilingFrameRotationOf(playerHorizontalFacing)
        else -> Rotation.NONE
    }
}

internal fun ceilingFrameRotationOf(playerHorizontalFacing: BlockFace): Rotation {
    return when (playerHorizontalFacing) {
        BlockFace.NORTH -> Rotation.NONE
        BlockFace.EAST -> Rotation.CLOCKWISE_135
        BlockFace.SOUTH -> Rotation.CLOCKWISE
        BlockFace.WEST -> Rotation.CLOCKWISE_45
        else -> error("Expected 4-way horizontal facing, got $playerHorizontalFacing")
    }
}

internal fun floorFrameRotationOf(playerHorizontalFacing: BlockFace): Rotation {
    return when (playerHorizontalFacing) {
        BlockFace.NORTH -> Rotation.NONE
        BlockFace.EAST -> Rotation.CLOCKWISE_45
        BlockFace.SOUTH -> Rotation.CLOCKWISE
        BlockFace.WEST -> Rotation.CLOCKWISE_135
        else -> error("Expected 4-way horizontal facing, got $playerHorizontalFacing")
    }
}

internal fun wallAxisOf(
    frameFacing: BlockFace,
    playerHorizontalFacing: BlockFace,
): WallAxis = when (frameFacing) {
    // 必须和 DisplayInstance.buildGeometry() 里的 axisU 保持一致，否则 tileX 会左右镜像
    BlockFace.NORTH -> WallAxis(
        right = BlockFace.WEST,
        down = BlockFace.DOWN,
    )

    BlockFace.SOUTH -> WallAxis(
        right = BlockFace.EAST,
        down = BlockFace.DOWN,
    )

    BlockFace.EAST -> WallAxis(
        right = BlockFace.NORTH,
        down = BlockFace.DOWN,
    )

    BlockFace.WEST -> WallAxis(
        right = BlockFace.SOUTH,
        down = BlockFace.DOWN,
    )

    // TODO: 修复水平放置
    // 地板：展示框朝上
    BlockFace.UP -> WallAxis(
        right = rightOf(playerHorizontalFacing),
        down = playerHorizontalFacing.oppositeFace,
    )

    // 天花板：展示框朝下
    BlockFace.DOWN -> WallAxis(
        right = rightOf(playerHorizontalFacing),
        down = playerHorizontalFacing,
    )

    else -> error("Unexpected item frame facing: $frameFacing")
}

private fun collectConnectedFrames(seed: ItemFrame, axis: WallAxis): Set<ItemFrame> {
    val queue = ArrayDeque<ItemFrame>()
    val visited = mutableSetOf<UUID>()
    val result = mutableSetOf<ItemFrame>()

    queue += seed
    visited += seed.uniqueId

    val directions = listOf(
        axis.right,
        axis.right.oppositeFace,
        axis.down,
        axis.down.oppositeFace,
    )

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        result += current

        for (direction in directions) {
            val neighbor = findAdjacentFrame(current, direction) ?: continue

            // 只接受同朝向的 frame，避免串到别的平面
            if (neighbor.facing != seed.facing) continue

            if (visited.add(neighbor.uniqueId)) {
                queue += neighbor
            }
        }
    }

    return result
}

private fun findAdjacentFrame(current: ItemFrame, direction: BlockFace): ItemFrame? {
    val targetBlock = current.location.block.getRelative(direction)
    return findItemFrameAt(current, targetBlock)
}

private fun findItemFrameAt(reference: ItemFrame, block: Block): ItemFrame? {
    val world = reference.world
    val chunk = block.chunk

    return chunk.entities
        .asSequence()
        .filterIsInstance<ItemFrame>()
        .firstOrNull { frame ->
            frame.world.uid == world.uid &&
                    frame.facing == reference.facing &&
                    frame.location.block.x == block.x &&
                    frame.location.block.y == block.y &&
                    frame.location.block.z == block.z
        }
}

private fun toGridPos(origin: ItemFrame, target: ItemFrame, axis: WallAxis): GridPos {
    val originBlock = origin.location.block
    val targetBlock = target.location.block

    val dx = targetBlock.x - originBlock.x
    val dy = targetBlock.y - originBlock.y
    val dz = targetBlock.z - originBlock.z

    val u = dx * axis.right.modX + dy * axis.right.modY + dz * axis.right.modZ
    val v = dx * axis.down.modX + dy * axis.down.modY + dz * axis.down.modZ

    return GridPos(u, v)
}

private fun findPlacement(
    framesByGridPos: Map<GridPos, ItemFrame>,
    width: Int,
    height: Int,
): Placement? {
    val preferredPlacements = listOf(
        Placement(left = 0, top = 0),
        Placement(left = -(width - 1), top = 0),
        Placement(left = 0, top = -(height - 1)),
        Placement(left = -(width - 1), top = -(height - 1)),
    ).distinct()

    for (placement in preferredPlacements) {
        if (isValidPlacement(framesByGridPos, placement, width, height)) {
            return placement
        }
    }

    for (left in -(width - 1)..0) {
        for (top in -(height - 1)..0) {
            val placement = Placement(left, top)
            if (placement in preferredPlacements) continue

            if (isValidPlacement(framesByGridPos, placement, width, height)) {
                return placement
            }
        }
    }

    return null
}

private fun isValidPlacement(
    framesByGridPos: Map<GridPos, ItemFrame>,
    placement: Placement,
    width: Int,
    height: Int,
): Boolean {
    for (tileY in 0 until height) {
        for (tileX in 0 until width) {
            val pos = GridPos(
                u = placement.left + tileX,
                v = placement.top + tileY,
            )

            val frame = framesByGridPos[pos] ?: return false

            if (!frame.item.type.isAir) {
                return false
            }
        }
    }

    return true
}
