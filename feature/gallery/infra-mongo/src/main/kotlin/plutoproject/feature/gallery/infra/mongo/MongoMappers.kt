package plutoproject.feature.gallery.infra.mongo

import org.bson.BsonBinary
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.image.StaticImageData
import plutoproject.feature.gallery.core.image.TilePool
import plutoproject.feature.gallery.infra.mongo.model.DisplayInstanceDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageTypeDocument
import plutoproject.feature.gallery.infra.mongo.model.ItemFrameFacingDocument

internal fun DisplayInstanceDocument.toDomain(): DisplayInstance {
    return DisplayInstance(
        id = id,
        belongsTo = belongsTo,
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
        belongsTo = belongsTo,
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
        mapWidthBlocks = mapWidthBlocks,
        mapHeightBlocks = mapHeightBlocks,
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
        mapWidthBlocks = mapWidthBlocks,
        mapHeightBlocks = mapHeightBlocks,
        tileMapIds = tileMapIds,
    )
}

internal fun ImageDataEntryDocument.toDomain(): ImageDataEntry<*> {
    return when (val domainType = type.toDomain()) {
        ImageType.STATIC -> ImageDataEntry(
            belongsTo = belongsTo,
            type = domainType,
            data = StaticImageData(
                tilePool = TilePool(
                    offsets = tilePoolOffsets,
                    blob = tilePoolBlob.data,
                ),
                tileIndexes = tileIndexes,
            ),
        )

        ImageType.ANIMATED -> ImageDataEntry(
            belongsTo = belongsTo,
            type = domainType,
            data = AnimatedImageData(
                frameCount = requireNotNull(frameCount) {
                    "frameCount is null for animated image data"
                },
                durationMillis = requireNotNull(durationMillis) {
                    "durationMillis is null for animated image data"
                },
                tilePool = TilePool(
                    offsets = tilePoolOffsets,
                    blob = tilePoolBlob.data,
                ),
                tileIndexes = tileIndexes,
            ),
        )
    }
}

internal fun ImageDataEntry<*>.toDocument(): ImageDataEntryDocument {
    return when (type) {
        ImageType.STATIC -> {
            val staticData = data as? StaticImageData
                ?: error("Image data type mismatch: expected StaticImageData, got ${data::class.simpleName}")

            ImageDataEntryDocument(
                belongsTo = belongsTo,
                type = type.toDocument(),
                tilePoolOffsets = staticData.tilePool.offsets,
                tilePoolBlob = BsonBinary(staticData.tilePool.blob),
                tileIndexes = staticData.tileIndexes,
                frameCount = null,
                durationMillis = null,
            )
        }

        ImageType.ANIMATED -> {
            val animatedData = data as? AnimatedImageData
                ?: error("Image data type mismatch: expected AnimatedImageData, got ${data::class.simpleName}")

            ImageDataEntryDocument(
                belongsTo = belongsTo,
                type = type.toDocument(),
                tilePoolOffsets = animatedData.tilePool.offsets,
                tilePoolBlob = BsonBinary(animatedData.tilePool.blob),
                tileIndexes = animatedData.tileIndexes,
                frameCount = animatedData.frameCount,
                durationMillis = animatedData.durationMillis,
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
