package plutoproject.foundation.common.bson

import java.util.UUID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import org.bson.BsonBinary
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder

@OptIn(ExperimentalSerializationApi::class)
object UuidAsBsonBinarySerializer : BsonSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("plutoproject.UuidAsBsonBinarySerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: BsonEncoder, value: UUID) {
        encoder.encodeBsonValue(BsonBinary(value))
    }

    override fun deserialize(decoder: BsonDecoder): UUID = decoder.decodeBsonValue().asBinary().asUuid()
}
