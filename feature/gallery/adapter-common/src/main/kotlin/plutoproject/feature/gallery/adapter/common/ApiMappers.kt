package plutoproject.feature.gallery.adapter.common

import plutoproject.feature.gallery.api.ImageDisplay
import plutoproject.feature.gallery.api.ItemFrameFacing
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.ItemFrameFacing as CoreItemFrameFacing
import java.util.UUID

fun DisplayInstance.toApi(): ImageDisplay = ImageDisplayImpl(
    id = id,
    imageId = imageId,
    world = world,
    chunkX = chunkX,
    chunkZ = chunkZ,
    facing = facing.toApi(),
    widthBlocks = widthBlocks,
    heightBlocks = heightBlocks,
    originX = originX,
    originY = originY,
    originZ = originZ,
    itemFrameIds = itemFrameIds,
)

private fun CoreItemFrameFacing.toApi(): ItemFrameFacing = when (this) {
    CoreItemFrameFacing.NORTH -> ItemFrameFacing.NORTH
    CoreItemFrameFacing.SOUTH -> ItemFrameFacing.SOUTH
    CoreItemFrameFacing.EAST -> ItemFrameFacing.EAST
    CoreItemFrameFacing.WEST -> ItemFrameFacing.WEST
    CoreItemFrameFacing.UP -> ItemFrameFacing.UP
    CoreItemFrameFacing.DOWN -> ItemFrameFacing.DOWN
}

private data class ImageDisplayImpl(
    override val id: UUID,
    override val imageId: UUID,
    override val world: String,
    override val chunkX: Int,
    override val chunkZ: Int,
    override val facing: ItemFrameFacing,
    override val widthBlocks: Int,
    override val heightBlocks: Int,
    override val originX: Double,
    override val originY: Double,
    override val originZ: Double,
    override val itemFrameIds: List<UUID>,
) : ImageDisplay
