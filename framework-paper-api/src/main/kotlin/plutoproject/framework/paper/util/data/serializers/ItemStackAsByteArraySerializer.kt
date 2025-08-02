package plutoproject.framework.paper.util.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.inventory.ItemStack

object ItemStackAsByteArraySerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

    override fun deserialize(decoder: Decoder): ItemStack {
        return ItemStack.deserializeBytes(ByteArraySerializer().deserialize(decoder))
    }

    override fun serialize(encoder: Encoder, value: ItemStack) {
        ByteArraySerializer().serialize(encoder, value.serializeAsBytes())
    }
}
