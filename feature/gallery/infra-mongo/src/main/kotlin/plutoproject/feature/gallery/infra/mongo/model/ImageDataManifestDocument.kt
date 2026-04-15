package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class ImageDataManifestDocument(
    @SerialName("_id")
    val imageId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val state: ImageDataManifestStateDocument,
    val chunkCount: Int,
    val schemaVersion: Int,
    val compression: ImageDataCompressionDocument,
    val encodedByteLength: Int,
)
