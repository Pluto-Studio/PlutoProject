package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class ImageDataEntryDocument(
    val belongsTo: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val type: ImageTypeDocument,
    val tilePoolOffsets: IntArray,
    val tilePoolBlob: ByteArray,
    val tileIndexes: ShortArray,
    val frameCount: Int? = null,
    val durationMillis: Int? = null,
)
