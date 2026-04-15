package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.BsonBinary
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class ImageDataChunkDocument(
    val imageId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val order: Int,
    val blob: @Contextual BsonBinary,
)
