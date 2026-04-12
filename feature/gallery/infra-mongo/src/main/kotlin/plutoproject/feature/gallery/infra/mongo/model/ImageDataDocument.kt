package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.*
import kotlin.time.Duration

@Serializable
data class ImageDataDocument(
    val imageId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val type: ImageTypeDocument,
    val tilePool: TilePoolDocument,
    val tileIndexes: ShortArray,

    // 仅 AnimatedImage
    val frameCount: Int? = null,
    val duration: Duration? = null,
) {
    init {
        when (type) {
            ImageTypeDocument.STATIC -> require(frameCount == null && duration == null) {
                "frameCount and duration must be null for static image"
            }

            ImageTypeDocument.ANIMATED -> require(frameCount != null && duration != null) {
                "frameCount and duration must not be null for animated image"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDataDocument

        if (frameCount != other.frameCount) return false
        if (imageId != other.imageId) return false
        if (type != other.type) return false
        if (tilePool != other.tilePool) return false
        if (!tileIndexes.contentEquals(other.tileIndexes)) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameCount ?: 0
        result = 31 * result + imageId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + tilePool.hashCode()
        result = 31 * result + tileIndexes.contentHashCode()
        result = 31 * result + (duration?.hashCode() ?: 0)
        return result
    }
}
