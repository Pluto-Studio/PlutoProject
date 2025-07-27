package plutoproject.feature.paper.exchangeshop

import com.mongodb.client.model.Updates
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.BsonBinary
import org.bson.conversions.Bson
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.api.exchangeshop.ShopTransactionParameters
import plutoproject.feature.paper.api.exchangeshop.TransactionFilter
import plutoproject.feature.paper.api.exchangeshop.TransactionFilterDsl
import plutoproject.feature.paper.exchangeshop.models.TransactionModel
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.toKotlinDuration

class ShopUserImpl(
    uniqueId: UUID,
    player: OfflinePlayer,
    ticket: Int,
    lastTicketRecoveryOn: Instant?,
    createdAt: Instant,
) : InternalShopUser, KoinComponent {
    private val config by inject<ExchangeShopConfig>()
    private val userRepo by inject<UserRepository>()
    private val transactionRepo by inject<TransactionRepository>()
    private val exchangeShop by inject<InternalExchangeShop>()
    private val saveLock = Mutex()
    private val isDirty = AtomicBoolean(false)
    private val internalTicket = AtomicInteger(ticket)
    private var scheduledTicketRecovery: Job? = null

    override val uniqueId: UUID = uniqueId
        get() {
            exchangeShop.setUsed(this)
            return field
        }
    override val player: OfflinePlayer = player
        get() {
            exchangeShop.setUsed(this)
            return field
        }
    override val createdAt: Instant = createdAt
        get() {
            exchangeShop.setUsed(this)
            return field
        }
    override var ticket: Int
        get() {
            exchangeShop.setUsed(this)
            return internalTicket.get()
        }
        set(value) {
            exchangeShop.setUsed(this)
            internalTicket.set(value)
            isDirty.set(true)
            updateTicketRecoverySchedule()
        }
    override var lastTicketRecoveryOn: Instant? = lastTicketRecoveryOn
        set(value) {
            exchangeShop.setUsed(this)
            field = value
        }
        get() {
            exchangeShop.setUsed(this)
            return field
        }
    override var nextTicketRecoveryOn: Instant? = lastTicketRecoveryOn?.plus(config.ticket.recoveryInterval)

    init {
        if (config.ticket.naturalRecovery) {
            exchangeShop.coroutineScope.launch {
                performOfflineRecovery()
                updateTicketRecoverySchedule()
            }
        }
    }

    private suspend fun performOfflineRecovery() {
        if (ticket >= config.ticket.recoveryCap) {
            return
        }

        val lastRecoveryTime = lastTicketRecoveryOn ?: createdAt
        val currentTime = Instant.now()
        val timeDelta = Duration.between(lastRecoveryTime, currentTime.plusSeconds(1))
        val recoveryAmount = config.ticket.recoveryAmount * timeDelta.dividedBy(config.ticket.recoveryInterval)

        if (lastRecoveryTime.isAfter(currentTime)) {
            if (createdAt.isAfter(currentTime)) {
                featureLogger.severe("Time anomaly detected for player ${player.name}: create time ($createdAt) is after current time ($currentTime), possibly due to system time change")
            }
            if (lastTicketRecoveryOn?.isAfter(currentTime) == true) {
                featureLogger.severe("Time anomaly detected for player ${player.name}: last recovery time ($lastTicketRecoveryOn) is after current time ($currentTime), possibly due to system time change")
                lastTicketRecoveryOn = currentTime
                isDirty.set(true)
                exchangeShop.coroutineScope.launch(Dispatchers.IO) {
                    save()
                }
            }
            return
        }

        recoveryTicket(recoveryAmount.toInt())
        save()
    }

    private fun recoveryTicket(amount: Int): Boolean {
        if (ticket >= config.ticket.recoveryCap) return false
        ticket = if (ticket + amount > config.ticket.recoveryCap) {
            config.ticket.recoveryCap
        } else {
            ticket + amount
        }
        lastTicketRecoveryOn = Instant.now()
        return true
    }

    private fun getNextRecoveryTime(): Instant {
        val lastRecoveryTimes = lastTicketRecoveryOn ?: createdAt
        return lastRecoveryTimes.plus(config.ticket.recoveryInterval)
    }

    private fun scheduleTicketRecovery() {
        if (ticket >= config.ticket.recoveryCap) return
        val currentTime = Instant.now()
        val nextRecoveryTime = getNextRecoveryTime()

        check(config.ticket.naturalRecovery) { "Natural ticket recovery is disabled" }
        require(nextRecoveryTime.isAfter(currentTime)) { "Ticket recovery must be scheduled in the future" }

        val interval = Duration.between(currentTime, nextRecoveryTime.plusSeconds(1))
        nextTicketRecoveryOn = nextRecoveryTime

        exchangeShop.coroutineScope.launch {
            delay(interval.toKotlinDuration())
            if (recoveryTicket(config.ticket.recoveryAmount)) {
                save()
            }
            scheduleTicketRecovery()
        }.also { scheduledTicketRecovery = it }
    }

    private fun unscheduleTicketRecovery() {
        if (scheduledTicketRecovery == null) return
        scheduledTicketRecovery?.cancel()
        scheduledTicketRecovery = null
        nextTicketRecoveryOn = null
    }

    private fun updateTicketRecoverySchedule() {
        if (ticket >= config.ticket.recoveryCap) {
            unscheduleTicketRecovery()
            return
        }
        if (scheduledTicketRecovery == null && config.ticket.naturalRecovery) {
            scheduleTicketRecovery()
        }
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

    override fun withdrawTicket(amount: Int): Int {
        exchangeShop.setUsed(this)
        require(ticket >= amount) { "Insufficient tickets for `$uniqueId`, only $ticket left" }
        val value = internalTicket.addAndGet(-amount)
        isDirty.set(true)
        updateTicketRecoverySchedule()
        return value
    }

    override fun depositTicket(amount: Int): Int {
        exchangeShop.setUsed(this)
        val value = internalTicket.addAndGet(amount)
        isDirty.set(true)
        updateTicketRecoverySchedule()
        return value
    }

    override fun findTransactions(
        skip: Int?,
        limit: Int?,
        filterBlock: TransactionFilterDsl.() -> Unit
    ): Flow<ShopTransaction> {
        exchangeShop.setUsed(this)
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
        exchangeShop.setUsed(this)
        val filter = TransactionFilterDsl().apply {
            TransactionFilter.PlayerId eq uniqueId
            filterBlock()
        }.build()
        return transactionRepo.count(filter)
    }

    override suspend fun makeTransaction(
        shopItemId: String,
        count: Int,
        checkAvailability: Boolean
    ): Result<ShopTransaction> {
        exchangeShop.setUsed(this)
        if (isDirty.get()) save()
        TODO("Not yet implemented")
    }

    override suspend fun batchTransaction(purchases: Map<String, ShopTransactionParameters>): Map<String, Result<ShopTransaction>> {
        return purchases.entries.associate { (shopItemId, parameters) ->
            shopItemId to makeTransaction(shopItemId, parameters.count, parameters.checkAvailability)
        }
    }

    override suspend fun save() = saveLock.withLock {
        exchangeShop.setUsed(this)
        if (!isDirty.get()) return
        val updates = Updates.combine(
            Updates.set("ticket", internalTicket.get()),
            Updates.set("lastTicketRecoveryOn", lastTicketRecoveryOn)
        )
        userRepo.update(uniqueId, updates)
        isDirty.set(false)
    }

    override suspend fun close() {
        scheduledTicketRecovery?.cancelAndJoin()
    }
}
