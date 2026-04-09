package plutoproject.feature.gallery.adapter.paper.listener

import ink.pmc.advkt.component.text
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
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.adapter.paper.*
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageStore
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
    private val coroutineScope = koin.get<CoroutineScope>()

    @EventHandler
    suspend fun onItemFrameChange(event: PlayerItemFrameChangeEvent) {
        if (event.itemStack.type != IMAGE_ITEM_MATERIAL
            || event.itemStack.imageItemData() == null
            || event.action == ROTATE
        ) {
            return
        }

        when (event.action) {
            PLACE -> onPlace(event)
            REMOVE -> onRemove(event.itemFrame, event.itemStack)
            ROTATE -> return
        }
    }

    private suspend fun onPlace(event: PlayerItemFrameChangeEvent) {
        val player = event.player
        val itemFrame = event.itemFrame
        val itemStack = event.itemStack

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
        event.setItemStack(ItemStack.empty())

        val image = imageDeferred.await()
        val imageData = imageDataDeferred.await()

        if (image == null || imageData == null) {
            if (image != null) {
                logger.warning("Cannot find image data for existed image \"${image.name}\" (${image.id})")
            }
            player.playSound(PLACEMENT_FAILED_SOUND)
            player.sendMessage(IMAGE_ITEM_PLACEMENT_FAILED_INVALID)
            return
        }

        val origin = selectedFrames.first().itemFrame
        val originCenter = origin.location.toCenterLocation()
        val displayInstance = DisplayInstance(
            id = UUID.randomUUID(),
            imageId = image.id,
            world = itemFrame.world.name,
            chunkX = origin.chunk.x,
            chunkZ = origin.chunk.z,
            facing = origin.facing.itemFrameFacing(),
            widthBlocks = image.widthBlocks,
            heightBlocks = image.heightBlocks,
            originX = originCenter.x,
            originY = originCenter.y,
            originZ = originCenter.z,
            itemFrameIds = selectedFrames.map { it.itemFrame.uniqueId }
        )

        coroutineScope.launch(Dispatchers.IO) {
            withTimeout(DISPLAY_INSTANCE_SAVE_TIMEOUT_SECONDS.seconds) {
                displayInstanceStore.create(displayInstance)
            }
        }

        selectedFrames.forEachIndexed { index, frame ->
            val tileIndex = frame.tileY * width + frame.tileX
            val mapId = image.tileMapIds[tileIndex]
            val nextFrame = selectedFrames.getOrNull(index + 1)
            val itemStack = ItemStack(Material.FILLED_MAP).apply {
                setData(DataComponentTypes.MAP_ID, MapId.mapId(mapId))
            }
            frame.itemFrame.setItem(itemStack)
            frame.itemFrame.setImageItemFrame(displayInstance, nextFrame?.itemFrame?.uniqueId)
        }

        displayRuntime.attach(image, imageData, displayInstance)
    }

    private fun onRemove(itemFrame: ItemFrame, itemStack: ItemStack) {
    }
}

private data class WallAxis(
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

/**
 * 把玩家 facing 归一化成纯四方向。
 *
 * 原因：
 * - Player#getFacing() 可能返回 16 方位里的值
 * - 但我们这里的局部坐标系只接受 NORTH/SOUTH/EAST/WEST
 *
 * 做法：
 * - 取 facing 在 XZ 平面的主轴方向
 * - 哪个轴绝对值更大，就归到哪个主方向
 */
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

/**
 * 计算“某个四方向朝向的右边”。
 */
private fun rightOf(facing: BlockFace): BlockFace = when (facing) {
    BlockFace.NORTH -> BlockFace.EAST
    BlockFace.EAST -> BlockFace.SOUTH
    BlockFace.SOUTH -> BlockFace.WEST
    BlockFace.WEST -> BlockFace.NORTH
    else -> error("Expected 4-way horizontal facing, got $facing")
}

/**
 * 根据展示框朝向，定义这张图所在平面的局部坐标轴。
 *
 * 规则：
 * - 墙面：使用固定阅读方向
 * - 地板/天花板：相对于玩家
 *   - right = 玩家右边
 *   - down  = 玩家正前方（XZ 平面的 facing）
 */
private fun wallAxisOf(
    frameFacing: BlockFace,
    playerHorizontalFacing: BlockFace,
): WallAxis = when (frameFacing) {
    BlockFace.NORTH -> WallAxis(
        right = BlockFace.EAST,
        down = BlockFace.DOWN,
    )

    BlockFace.SOUTH -> WallAxis(
        right = BlockFace.WEST,
        down = BlockFace.DOWN,
    )

    BlockFace.EAST -> WallAxis(
        right = BlockFace.SOUTH,
        down = BlockFace.DOWN,
    )

    BlockFace.WEST -> WallAxis(
        right = BlockFace.NORTH,
        down = BlockFace.DOWN,
    )

    // 地板：展示框朝上
    BlockFace.UP -> WallAxis(
        right = rightOf(playerHorizontalFacing),
        down = playerHorizontalFacing,
    )

    // 天花板：展示框朝下
    BlockFace.DOWN -> WallAxis(
        right = rightOf(playerHorizontalFacing),
        down = playerHorizontalFacing,
    )

    else -> error("Unexpected item frame facing: $frameFacing")
}

/**
 * 从 seed 开始，在当前展示平面里做 4 邻接 BFS，
 * 收集同朝向、连通的全部 ItemFrame。
 *
 * 注意：
 * 这里只关心“有没有 frame”，不关心它是否为空。
 * 空不空由 placement 检查负责。
 */
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

/**
 * 找 current 在某个“平面方向”上的相邻展示框。
 */
private fun findAdjacentFrame(current: ItemFrame, direction: BlockFace): ItemFrame? {
    val targetBlock = current.location.block.getRelative(direction)
    return findItemFrameAt(current, targetBlock)
}

/**
 * 在指定方块位置上找一个和 reference 同朝向的 ItemFrame。
 *
 * Bukkit 没有 block -> item frame 的直接索引，
 * 这里先从 block 所在 chunk 的实体里过滤。
 */
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

/**
 * 把 target frame 投影成相对于 origin 的局部二维网格坐标。
 *
 * 约定：
 * - origin 自己就是 (0, 0)
 * - 沿 right 方向每走一格，u + 1
 * - 沿 down 方向每走一格，v + 1
 */
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

/**
 * 在所有“包含点击框 (0,0)” 的 width * height placement 里找第一个合法的。
 *
 * 先试四个角，再试其它包含点击点的情况。
 */
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

/**
 * 检查一个 placement 是否合法。
 *
 * 合法条件：
 * - 矩形内每个格子都必须存在 ItemFrame
 * - 且每个 ItemFrame 都必须为空
 */
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
