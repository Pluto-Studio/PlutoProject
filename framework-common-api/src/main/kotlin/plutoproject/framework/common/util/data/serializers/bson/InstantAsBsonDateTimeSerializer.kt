package plutoproject.framework.common.util.data.serializers.bson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import org.bson.BsonDateTime
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
object InstantAsBsonDateTimeSerializer : BsonSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("plutoproject.InstantAsBsonDateTimeSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: BsonEncoder, value: Instant) {
        encoder.encodeBsonValue(BsonDateTime(value.toEpochMilli()))
    }

    override fun deserialize(decoder: BsonDecoder): Instant {
        return Instant.ofEpochMilli(decoder.decodeBsonValue().asDateTime().value)
    }
}
