package plutoproject.framework.common.util.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.ByteBuffer
import java.util.UUID

object UuidAsByteArraySerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

    override fun serialize(encoder: Encoder, value: UUID) {
        val buffer = ByteBuffer.allocate(16)
        buffer.putLong(value.mostSignificantBits)
        buffer.putLong(value.leastSignificantBits)
        ByteArraySerializer().serialize(encoder, buffer.array())
    }

    override fun deserialize(decoder: Decoder): UUID {
        val bytes = ByteArraySerializer().deserialize(decoder)
        val buffer = ByteBuffer.wrap(bytes)
        val mostSigBits = buffer.long
        val leastSigBits = buffer.long
        return UUID(mostSigBits, leastSigBits)
    }
}
