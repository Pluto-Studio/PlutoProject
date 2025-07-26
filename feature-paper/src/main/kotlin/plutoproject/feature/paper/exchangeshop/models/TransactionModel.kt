package plutoproject.feature.paper.exchangeshop.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.framework.common.util.data.serializers.bson.BigDecimalAsBsonDecimal128Serializer
import plutoproject.framework.common.util.data.serializers.bson.InstantAsBsonDateTimeSerializer
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import plutoproject.framework.paper.util.data.serializers.bson.ItemStackAsBsonBinarySerializer
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Serializable
data class TransactionModel(
    @SerialName("_id") val id: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val playerId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val time: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant,
    val itemId: String,
    val material: Material,
    val itemStack: @Serializable(ItemStackAsBsonBinarySerializer::class) ItemStack,
    val amount: Int,
    val quantity: Int,
    val ticket: Int,
    val cost: @Serializable(BigDecimalAsBsonDecimal128Serializer::class) BigDecimal,
    val balance: @Serializable(BigDecimalAsBsonDecimal128Serializer::class) BigDecimal,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransactionModel
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
