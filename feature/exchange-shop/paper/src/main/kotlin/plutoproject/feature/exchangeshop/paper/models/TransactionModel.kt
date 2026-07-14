package plutoproject.feature.exchangeshop.paper.models

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.BsonBinary
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import plutoproject.foundation.common.bson.BigDecimalAsBsonDecimal128Serializer
import plutoproject.foundation.common.serialization.InstantAsBsonDateTimeSerializer
import plutoproject.foundation.common.bson.UuidAsBsonBinarySerializer
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Serializable
data class TransactionModel(
    @SerialName("_id") val id: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val playerId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val time: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant,
    val shopItemId: String,
    @SerialName("itemType") val itemTypeString: String,
    @SerialName("itemStack") val itemStackBinary: @Contextual BsonBinary,
    val amount: Int,
    val quantity: Int,
    val ticket: Long,
    val cost: @Serializable(BigDecimalAsBsonDecimal128Serializer::class) BigDecimal,
    val balance: @Serializable(BigDecimalAsBsonDecimal128Serializer::class) BigDecimal,
) {
    val itemType: ItemType?
        get() = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ITEM)
            .get(NamespacedKey(itemTypeString.substringBefore(":"), itemTypeString.substringAfter(":")))
    val itemStack: ItemStack?
        get() = runCatching { ItemStack.deserializeBytes(itemStackBinary.data) }.getOrNull()

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
