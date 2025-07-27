package plutoproject.feature.paper.exchangeshop

import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.exchangeshop.models.TransactionModel
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class ShopTransactionImpl(
    override val id: UUID,
    override val playerId: UUID,
    override val time: Instant,
    override val shopItemId: String,
    override val itemStack: ItemStack?,
    override val amount: Int,
    override val quantity: Int,
    override val ticket: Int,
    override val cost: BigDecimal,
    override val balance: BigDecimal
) : ShopTransaction {
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
}
