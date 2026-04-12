package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class DisplayInstanceDocument(
    val id: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val imageId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val world: String,
    val chunkX: Int,
    val chunkZ: Int,
    val facing: ItemFrameFacingDocument,
    val widthBlocks: Int,
    val heightBlocks: Int,
    val originX: Double,
    val originY: Double,
    val originZ: Double,
    val itemFrameIds: List<@Serializable(UuidAsBsonBinarySerializer::class) UUID>,
)
