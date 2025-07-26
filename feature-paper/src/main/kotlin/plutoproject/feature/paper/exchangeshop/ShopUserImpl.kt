package plutoproject.feature.paper.exchangeshop

import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.BsonBinary
import org.bson.conversions.Bson
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.api.exchangeshop.TransactionFilter
import plutoproject.feature.paper.api.exchangeshop.TransactionFilterDsl
import plutoproject.feature.paper.exchangeshop.models.TransactionModel
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import java.time.Instant
import java.util.*

class ShopUserImpl(
    override val uniqueId: UUID,
    override val player: OfflinePlayer,
    override var ticket: Int,
    lastTicketRecoveryOn: Instant?,
) : ShopUser, KoinComponent {
    private val userRepo by inject<UserRepository>()
    private val transactionRepo by inject<TransactionRepository>()
    private val exchangeShop by inject<InternalExchangeShop>()
    private val updateLock = Mutex()
    private val pendingUpdates = mutableListOf<Bson>()

    override var lastTicketRecoveryOn: Instant? = lastTicketRecoveryOn
        set(value) {
            exchangeShop.setUsed(this)
            field = value
        }
        get() {
            exchangeShop.setUsed(this)
            return field
        }

    private fun addTicketSetUpdate(amount: Int) {
        pendingUpdates.add(Updates.set("ticket", amount))
    }

    private suspend fun patchItem(model: TransactionModel) {
        val updates = mutableListOf<Bson>()
        val itemStack = model.itemStack ?: return
        val itemStackBinary = itemStack.serializeAsBytes()
        val itemType = itemStack.type.asItemType()

        if (itemType == null) {
            featureLogger.warning("Patching item '${model.itemTypeString}' failed: unknown material '${itemStack.type}'")
            return
        }
        if (!itemStackBinary.contentEquals(model.itemStackBinary.data)) {
            featureLogger.info("Patching item '${model.itemTypeString}': item stack binary updated")
            updates.add(Updates.set("itemStack", BsonBinary(itemStackBinary)))
        }
        if (itemType != model.itemType) {
            featureLogger.info("Patching item '${model.itemTypeString}': item type updated to '${itemType.key}'")
            updates.add(Updates.set("itemType", itemType.key.toString()))
        }

        if (updates.isEmpty()) return
        transactionRepo.update(model.id, Updates.combine(updates))
    }

    override suspend fun withdrawTicket(amount: Int): Int {
        require(ticket >= amount) { "Insufficient tickets for `$uniqueId`, only $ticket left" }
        return setTicket(ticket - amount)
    }

    override suspend fun depositTicket(amount: Int): Int {
        return setTicket(ticket + amount)
    }

    override suspend fun setTicket(amount: Int): Int {
        updateLock.withLock {
            ticket = amount
            addTicketSetUpdate(ticket)
        }
        return ticket
    }

    override fun findTransactions(
        skip: Int?,
        limit: Int?,
        filterBlock: TransactionFilterDsl.() -> Unit
    ): Flow<ShopTransaction> {
        val filter = TransactionFilterDsl().apply {
            TransactionFilter.PlayerId eq uniqueId
            filterBlock()
        }.build()
        return transactionRepo.find(filter, skip, limit).map { model ->
            exchangeShop.coroutineScope.launch(Dispatchers.IO) {
                patchItem(model)
            }
            if (model.itemStack == null) {
                featureLogger.warning("Unknown item stack: ${model.itemTypeString}")
            }
            ShopTransactionImpl(
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
    }

    override suspend fun countTransactions(filterBlock: TransactionFilterDsl.() -> Unit): Long {
        val filter = TransactionFilterDsl().apply {
            TransactionFilter.PlayerId eq uniqueId
            filterBlock()
        }.build()
        return transactionRepo.count(filter)
    }

    override suspend fun makeTransaction(shopItemId: String, count: Int): Result<ShopTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun batchTransaction(purchases: Map<String, Int>): Map<String, Result<ShopTransaction>> {
        return purchases.entries.associate { (shopItemId, count) ->
            shopItemId to makeTransaction(shopItemId, count)
        }
    }

    override suspend fun save() {
        updateLock.withLock {
            userRepo.update(uniqueId, Updates.combine(pendingUpdates))
            pendingUpdates.clear()
        }
    }
}
