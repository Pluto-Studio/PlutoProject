package plutoproject.framework.common.util.data.serializers.bson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder

@OptIn(ExperimentalSerializationApi::class)
interface BsonSerializer<T> : KSerializer<T> {
    fun serialize(encoder: BsonEncoder, value: T)

    fun deserialize(decoder: BsonDecoder): T

    override fun serialize(encoder: Encoder, value: T) {
        when (encoder) {
            is BsonEncoder -> serialize(encoder, value)
            else -> throw SerializationException("Bson values are not supported by ${this::class}")
        }
    }

    override fun deserialize(decoder: Decoder): T {
        return when (decoder) {
            is BsonDecoder -> deserialize(decoder)
            else -> throw SerializationException("Bson values are not supported by ${this::class}")
        }
    }
}
