package plutoproject.framework.common.util.data.serializers.bson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import org.bson.BsonDecimal128
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import org.bson.types.Decimal128
import java.math.BigDecimal

@OptIn(ExperimentalSerializationApi::class)
object BigDecimalAsBsonDecimal128Serializer : BsonSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("plutoproject.BigDecimalAsBsonDecimal128Serializer", PrimitiveKind.LONG)

    override fun serialize(encoder: BsonEncoder, value: BigDecimal) {
        encoder.encodeBsonValue(BsonDecimal128(Decimal128(value)))
    }

    override fun deserialize(decoder: BsonDecoder): BigDecimal {
        return decoder.decodeBsonValue().asDecimal128().value.bigDecimalValue()
    }
}
