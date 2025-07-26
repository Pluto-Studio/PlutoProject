package plutoproject.feature.paper.exchangeshop

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.conversions.Bson
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.api.exchangeshop.TransactionFilterDsl
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

    override suspend fun findTransactions(
        skip: Int?,
        limit: Int?,
        filterBlock: TransactionFilterDsl.() -> Unit
    ): Flow<ShopTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun countTransactions(filterBlock: TransactionFilterDsl.() -> Unit): Long {
        val filter = TransactionFilterDsl().apply(filterBlock).build()
        return transactionRepo.count(Filters.and(Filters.eq("playerId", uniqueId), filter))
    }

    override suspend fun makeTransaction(itemId: String, count: Int): Result<ShopTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun batchTransaction(purchases: Map<String, Int>): Map<String, Result<ShopTransaction>> {
        TODO("Not yet implemented")
    }

    override suspend fun save() {
        updateLock.withLock {
            userRepo.update(uniqueId, Updates.combine(pendingUpdates))
            pendingUpdates.clear()
        }
    }
}
