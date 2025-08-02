package plutoproject.framework.paper.util.data.serializers.bson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import org.bson.BsonBinary
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import org.bukkit.inventory.ItemStack
import plutoproject.framework.common.util.data.serializers.bson.BsonSerializer

@OptIn(ExperimentalSerializationApi::class)
object ItemStackAsBsonBinarySerializer : BsonSerializer<ItemStack> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("plutoproject.ItemStackAsBsonBinarySerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: BsonEncoder, value: ItemStack) {
        encoder.encodeBsonValue(BsonBinary(value.serializeAsBytes()))
    }

    override fun deserialize(decoder: BsonDecoder): ItemStack {
        return ItemStack.deserializeBytes(decoder.decodeBsonValue().asBinary().data)
    }
}
