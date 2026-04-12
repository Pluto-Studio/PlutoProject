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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDocument

        if (mapWidthBlocks != other.mapWidthBlocks) return false
        if (mapHeightBlocks != other.mapHeightBlocks) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (owner != other.owner) return false
        if (ownerName != other.ownerName) return false
        if (name != other.name) return false
        if (!tileMapIds.contentEquals(other.tileMapIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mapWidthBlocks
        result = 31 * result + mapHeightBlocks
        result = 31 * result + id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + ownerName.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + tileMapIds.contentHashCode()
        return result
    }
}
