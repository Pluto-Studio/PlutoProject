package plutoproject.feature.gallery.infra.mongo

import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.ImageType
import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.TilePool
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import plutoproject.feature.gallery.infra.mongo.model.ImageTypeDocument

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
    val domainType = type.toDomain()
    return when (domainType) {
        ImageType.STATIC -> ImageDataEntry(
            belongsTo = belongsTo,
            type = domainType,
            data = StaticImageData(
                tilePool = TilePool(
                    offsets = tilePoolOffsets,
                    blob = tilePoolBlob,
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
                    blob = tilePoolBlob,
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
                tilePoolBlob = staticData.tilePool.blob,
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
                tilePoolBlob = animatedData.tilePool.blob,
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
