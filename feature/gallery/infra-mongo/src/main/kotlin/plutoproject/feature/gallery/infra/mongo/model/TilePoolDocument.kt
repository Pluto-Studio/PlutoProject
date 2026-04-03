package plutoproject.feature.gallery.infra.mongo.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.BsonBinary

@Serializable
data class TilePoolDocument(
    val offset: IntArray,
    val blob: @Contextual BsonBinary // ByteArray 序列化到数据库里之后是一个数字数组，单独换成 Bson 类型来获得更紧凑的存储。
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TilePoolDocument

        if (!offset.contentEquals(other.offset)) return false
        if (blob != other.blob) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.contentHashCode()
        result = 31 * result + blob.hashCode()
        return result
    }
}
