package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.BsonBinary
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.util.UUID

@Serializable
data class ImageDataEntryDocument(
    val belongsTo: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val type: ImageTypeDocument,
    val tilePoolOffsets: IntArray,
    val tilePoolBlob: @Contextual BsonBinary, // ByteArray 序列化到数据库里之后是一个数字数组，单独换成 Bson 类型来获得更紧凑的存储。
    val tileIndexes: ShortArray,
    val frameCount: Int? = null,
    val durationMillis: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDataEntryDocument

        if (frameCount != other.frameCount) return false
        if (durationMillis != other.durationMillis) return false
        if (belongsTo != other.belongsTo) return false
        if (type != other.type) return false
        if (!tilePoolOffsets.contentEquals(other.tilePoolOffsets)) return false
        if (tilePoolBlob != other.tilePoolBlob) return false
        if (!tileIndexes.contentEquals(other.tileIndexes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameCount ?: 0
        result = 31 * result + (durationMillis ?: 0)
        result = 31 * result + belongsTo.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + tilePoolOffsets.contentHashCode()
        result = 31 * result + tilePoolBlob.hashCode()
        result = 31 * result + tileIndexes.contentHashCode()
        return result
    }
}
