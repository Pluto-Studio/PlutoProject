package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class ImageDocument(
    val id: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val type: ImageTypeDocument,
    val owner: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val ownerName: String,
    val name: String,
    val mapWidthBlocks: Int,
    val mapHeightBlocks: Int,
    val tileMapIds: IntArray,
)
