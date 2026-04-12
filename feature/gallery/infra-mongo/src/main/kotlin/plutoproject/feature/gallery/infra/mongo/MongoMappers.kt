package plutoproject.feature.gallery.infra.mongo

import org.bson.BsonBinary
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import plutoproject.feature.gallery.infra.mongo.model.*

internal fun DisplayInstanceDocument.toDomain(): DisplayInstance {
    return DisplayInstance(
        id = id,
        imageId = imageId,
        world = world,
        chunkX = chunkX,
        chunkZ = chunkZ,
        facing = facing.toDomain(),
        widthBlocks = widthBlocks,
        heightBlocks = heightBlocks,
        originX = originX,
        originY = originY,
        originZ = originZ,
        itemFrameIds = itemFrameIds,
    )
}

internal fun DisplayInstance.toDocument(): DisplayInstanceDocument {
    return DisplayInstanceDocument(
        id = id,
        imageId = imageId,
        world = world,
        chunkX = chunkX,
        chunkZ = chunkZ,
        facing = facing.toDocument(),
        widthBlocks = widthBlocks,
        heightBlocks = heightBlocks,
        originX = originX,
        originY = originY,
        originZ = originZ,
        itemFrameIds = itemFrameIds,
    )
}

internal fun ImageDocument.toDomain(): Image {
    return Image(
        id = id,
        type = type.toDomain(),
        owner = owner,
        ownerName = ownerName,
        name = name,
        widthBlocks = mapWidthBlocks,
        heightBlocks = mapHeightBlocks,
        tileMapIds = tileMapIds,
    )
}

internal fun Image.toDocument(): ImageDocument {
    return ImageDocument(
        id = id,
        type = type.toDocument(),
        owner = owner,
        ownerName = ownerName,
        name = name,
        mapWidthBlocks = widthBlocks,
        mapHeightBlocks = heightBlocks,
        tileMapIds = tileMapIds,
    )
}

internal fun ImageDataDocument.toDomain(): ImageData {
    return when (type.toDomain()) {
        ImageType.STATIC -> ImageData.Static(
            tilePool = TilePool.fromSnapshot(TilePoolSnapshot(tilePool.offset, tilePool.blob.data)),
            tileIndexes = tileIndexes,
        )

        ImageType.ANIMATED -> ImageData.Animated(
            tilePool = TilePool.fromSnapshot(TilePoolSnapshot(tilePool.offset, tilePool.blob.data)),
            tileIndexes = tileIndexes,
            frameCount = requireNotNull(frameCount) {
                "frameCount must not be null for animated image"
            },
            duration = requireNotNull(duration) {
                "duration must not be null for animated image"
            },
        )
    }
}

internal fun ImageData.toDocument(imageId: java.util.UUID): ImageDataDocument {
    return when (this) {
        is ImageData.Static -> {
            val snapshot = tilePool.snapshot()
            ImageDataDocument(
                imageId = imageId,
                type = type.toDocument(),
                tilePool = TilePoolDocument(snapshot.offsets, BsonBinary(snapshot.blob)),
                tileIndexes = tileIndexes,
                frameCount = null,
                duration = null,
            )
        }

        is ImageData.Animated -> {
            val snapshot = tilePool.snapshot()
            ImageDataDocument(
                imageId = imageId,
                type = type.toDocument(),
                tilePool = TilePoolDocument(snapshot.offsets, BsonBinary(snapshot.blob)),
                tileIndexes = tileIndexes,
                frameCount = frameCount,
                duration = duration,
            )
        }
    }
}

internal fun ImageTypeDocument.toDomain(): ImageType {
    return when (this) {
        ImageTypeDocument.STATIC -> ImageType.STATIC
        ImageTypeDocument.ANIMATED -> ImageType.ANIMATED
    }
}

internal fun ImageType.toDocument(): ImageTypeDocument {
    return when (this) {
        ImageType.STATIC -> ImageTypeDocument.STATIC
        ImageType.ANIMATED -> ImageTypeDocument.ANIMATED
    }
}

internal fun ItemFrameFacingDocument.toDomain(): ItemFrameFacing {
    return when (this) {
        ItemFrameFacingDocument.NORTH -> ItemFrameFacing.NORTH
        ItemFrameFacingDocument.SOUTH -> ItemFrameFacing.SOUTH
        ItemFrameFacingDocument.EAST -> ItemFrameFacing.EAST
        ItemFrameFacingDocument.WEST -> ItemFrameFacing.WEST
        ItemFrameFacingDocument.UP -> ItemFrameFacing.UP
        ItemFrameFacingDocument.DOWN -> ItemFrameFacing.DOWN
    }
}

internal fun ItemFrameFacing.toDocument(): ItemFrameFacingDocument {
    return when (this) {
        ItemFrameFacing.NORTH -> ItemFrameFacingDocument.NORTH
        ItemFrameFacing.SOUTH -> ItemFrameFacingDocument.SOUTH
        ItemFrameFacing.EAST -> ItemFrameFacingDocument.EAST
        ItemFrameFacing.WEST -> ItemFrameFacingDocument.WEST
        ItemFrameFacing.UP -> ItemFrameFacingDocument.UP
        ItemFrameFacing.DOWN -> ItemFrameFacingDocument.DOWN
    }
}
