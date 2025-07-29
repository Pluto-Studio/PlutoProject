package plutoproject.feature.paper.exchangeshop

import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.exchangeshop.models.TransactionModel
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class ShopTransactionImpl(
    override val id: UUID,
    override val playerId: UUID,
    override val time: Instant,
    override val shopItemId: String,
    itemStack: ItemStack?,
    override val amount: Int,
    override val quantity: Int,
    override val ticket: Long,
    override val cost: BigDecimal,
    override val balance: BigDecimal
) : ShopTransaction {
    private val _itemStack = itemStack
    override val itemStack: ItemStack?
        get() = _itemStack?.clone()

    constructor(model: TransactionModel) : this(
        id = model.id,
        playerId = model.playerId,
        time = model.time,
        shopItemId = model.shopItemId,
        itemStack = model.itemStack,
        amount = model.amount,
        quantity = model.quantity,
        ticket = model.ticket,
        cost = model.cost,
        balance = model.balance
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ShopTransaction
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
