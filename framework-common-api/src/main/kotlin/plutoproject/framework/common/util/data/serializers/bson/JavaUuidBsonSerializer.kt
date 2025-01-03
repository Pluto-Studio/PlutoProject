package plutoproject.framework.common.util.data.serializers.bson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonBinary
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
object JavaUuidBsonSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java_uuid_bson", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return when (decoder) {
            is BsonDecoder -> decoder.decodeBsonValue().asBinary().asUuid()
            else -> throw SerializationException("BsonValues are not supported by ${decoder::class}")
        }
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        when (encoder) {
            is BsonEncoder -> encoder.encodeBsonValue(BsonBinary(value))
            else -> throw SerializationException("BsonValues are not supported by ${encoder::class}")
        }
    }
}
