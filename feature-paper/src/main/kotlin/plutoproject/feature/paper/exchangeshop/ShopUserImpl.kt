package plutoproject.feature.paper.exchangeshop

import com.mongodb.client.model.Updates
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.BsonBinary
import org.bson.conversions.Bson
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.*
import plutoproject.feature.paper.exchangeshop.models.TransactionModel
import plutoproject.feature.paper.exchangeshop.models.UserModel
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.util.coroutine.createSupervisorChild
import plutoproject.framework.common.util.data.flow.getValue
import plutoproject.framework.common.util.data.flow.setValue
import plutoproject.framework.common.util.logger
import plutoproject.framework.common.util.time.times
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.hook.vaultHook
import plutoproject.framework.paper.util.inventory.addItemOrDrop
import plutoproject.framework.paper.util.server
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.time.toKotlinDuration

class ShopUserImpl(
    override val uniqueId: UUID,
    override val player: OfflinePlayer,
    override val createdAt: Instant,
    ticket: Int,
    override var lastTicketRecoveryTime: Instant?,
    override var scheduledTicketRecoveryTime: Instant?,
) : InternalShopUser, KoinComponent {
    private val config by inject<ExchangeShopConfig>()
    private val userRepo by inject<UserRepository>()
    private val transactionRepo by inject<TransactionRepository>()
    private val exchangeShop by inject<InternalExchangeShop>()
    private val ticketLock = Mutex()
    private val coroutineScope: CoroutineScope = exchangeShop.coroutineScope.createSupervisorChild() + Dispatchers.IO

    private var isDirty by MutableStateFlow(false)
    private var _ticket by MutableStateFlow(ticket)
    private var scheduledTicketRecovery: Job? = null

    constructor(model: UserModel) : this(
        uniqueId = model.uniqueId,
        player = server.getOfflinePlayer(model.uniqueId),
        createdAt = model.createTime,
        ticket = model.ticket,
        lastTicketRecoveryTime = model.lastTicketRecoveryTime,
        scheduledTicketRecoveryTime = null,
    )

    override var isValid by MutableStateFlow(true)
    override var ticket: Int
        get() = _ticket
        set(value) = coroutineScope.launch { setTicket(value) }.asCompletableFuture().join()
    override val fullTicketRecoveryTime: Instant?
        get() {
            if (ticket >= config.ticket.recoveryCap) return null
            val lastRecoveryTime = lastTicketRecoveryTime ?: createdAt
            val unrecoveredAmount = config.ticket.recoveryCap - ticket
            return lastRecoveryTime + config.ticket.recoveryInterval * unrecoveredAmount.toLong()
        }

    init {
        if (config.ticket.naturalRecovery) {
            coroutineScope.launch { initializeTicketRecovery() }
        }
    }

    private suspend fun initializeTicketRecovery() = ticketLock.withLock {
        // 在计划在线恢复前先完成离线恢复，在线恢复中获取到的值是已经完成离线恢复的
        performOfflineRecovery()
        updateTicketRecoverySchedule()
    }

    private suspend fun performOfflineRecovery() {
        if (ticket >= config.ticket.recoveryCap) return

        val lastOnlineRecoveryTime = lastTicketRecoveryTime ?: createdAt
        val completedIntervals = getCompletedIntervalsSince(lastOnlineRecoveryTime)
        val lastOfflineRecoveryTime =
            lastOnlineRecoveryTime + config.ticket.recoveryInterval * completedIntervals.toLong()
        val offlineRecoveryAmount = completedIntervals * config.ticket.recoveryAmount

        if (offlineRecoveryAmount <= 0) return

        _ticket = if (ticket + offlineRecoveryAmount > config.ticket.recoveryCap) {
            config.ticket.recoveryCap
        } else {
            ticket + offlineRecoveryAmount
        }
        isDirty = true
        lastTicketRecoveryTime = lastOfflineRecoveryTime

        saveWithoutLock()
    }

    private suspend fun recoveryTicket(amount: Int): Boolean {
        if (ticket >= config.ticket.recoveryCap) return false

        _ticket = if (ticket + amount > config.ticket.recoveryCap) {
            config.ticket.recoveryCap
        } else {
            ticket + amount
        }
        lastTicketRecoveryTime = Instant.now()
        isDirty = true

        saveWithoutLock()
        return true
    }

    private fun getCompletedIntervalsSince(time: Instant): Int {
        val currentTime = Instant.now()
        require(time.isBefore(currentTime)) { "Start timestamp must in before" }
        val timeSinceLastRecovery = Duration.between(time, currentTime)
        return timeSinceLastRecovery.dividedBy(config.ticket.recoveryInterval).toInt()
    }

    private fun calculateTimeUntilNextRecovery(): Duration {
        val currentTime = Instant.now()
        val lastRecoveryTime = lastTicketRecoveryTime ?: createdAt
        val timeSinceLastRecovery = Duration.between(lastRecoveryTime, currentTime)

        return if (timeSinceLastRecovery <= config.ticket.recoveryInterval) {
            // 距离上次恢复时间还不满一个间隔
            config.ticket.recoveryInterval.minus(timeSinceLastRecovery)
        } else {
            // 大于一个间隔，表明此前已回满过一次
            // 由于离线恢复会在在线恢复开始前完成，所以此处大于一个间隔一定是已经回满过一次
            config.ticket.recoveryInterval
        }
    }

    private suspend fun scheduleTicketRecovery() {
        check(scheduledTicketRecovery == null) { "There is a scheduled ticket recovery unfinished" }
        featureLogger.info("Scheduling ticket recovery for ${player.name} (isValid = $isValid, isActive = ${coroutineScope.isActive})")
        if (!isValid) return
        if (!config.ticket.naturalRecovery) return
        if (ticket >= config.ticket.recoveryCap) return

        val currentTime = Instant.now()
        val scheduledAt = currentTime + calculateTimeUntilNextRecovery()

        check(scheduledAt.isAfter(currentTime)) { "Ticket recovery must be scheduled in the future" }

        val interval = Duration.between(currentTime, scheduledAt)
        val keepScheduledAt = scheduledTicketRecoveryTime
        scheduledTicketRecoveryTime = scheduledAt

        if (scheduledTicketRecoveryTime != keepScheduledAt) {
            isDirty = true
            saveWithoutLock()
        }

        coroutineScope.launch {
            delay(interval.toKotlinDuration())
            ticketLock.withLock { performScheduledRecovery() }
        }.also { scheduledTicketRecovery = it }
        featureLogger.info("Scheduled ticket recovery for ${player.name} on $scheduledAt")
    }

    private suspend fun performScheduledRecovery() {
        if (recoveryTicket(config.ticket.recoveryAmount)) {
            featureLogger.info("Ticket recovery performed for ${player.name} (ticket = $ticket)")
        }
        scheduledTicketRecovery = null
        scheduledTicketRecoveryTime = null
        isDirty = true
        saveWithoutLock()
        // 还没有恢复满，继续计划恢复
        if (ticket < config.ticket.recoveryCap) {
            if (!coroutineScope.isActive || !isValid) return
            scheduleTicketRecovery()
        }
    }

    // 卸载 ShopUser 时在数据库里保存计划的恢复任务，下次加载时若未截止就继续它
    private suspend fun unscheduleTicketRecovery() {
        if (scheduledTicketRecovery == null) return
        scheduledTicketRecovery?.cancelAndJoin()
        scheduledTicketRecovery = null
        scheduledTicketRecoveryTime = null
        featureLogger.info("Unscheduled ticket recovery for ${player.name}")
    }

    private suspend fun updateTicketRecoverySchedule() {
        if (scheduledTicketRecovery != null && ticket >= config.ticket.recoveryCap) {
            logger.info("Ticket remaining enough, unschedule")
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

    override suspend fun withdrawTicket(amount: Int): Int = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        require(ticket >= amount) { "Insufficient tickets for `$uniqueId`, only $ticket left" }
        _ticket -= amount
        isDirty = true
        updateTicketRecoverySchedule()
        return _ticket
    }

    override suspend fun depositTicket(amount: Int): Int = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        _ticket += amount
        isDirty = true
        updateTicketRecoverySchedule()
        return _ticket
    }

    override suspend fun setTicket(amount: Int): Int = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        _ticket = amount
        isDirty = true
        updateTicketRecoverySchedule()
        return _ticket
    }

    override fun findTransactions(
        skip: Int?,
        limit: Int?,
        filterBlock: TransactionFilterDsl.() -> Unit
    ): Flow<ShopTransaction> {
        check(isValid) { "Instance not valid" }
        val filter = TransactionFilterDsl().apply {
            TransactionFilter.PlayerId eq uniqueId
            filterBlock()
        }.build()
        return transactionRepo.find(filter, skip, limit).map { model ->
            coroutineScope.launch {
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
        check(isValid) { "Instance not valid" }
        val filter = TransactionFilterDsl().apply {
            TransactionFilter.PlayerId eq uniqueId
            filterBlock()
        }.build()
        return transactionRepo.count(filter)
    }

    override suspend fun makeTransaction(
        shopItem: ShopItem,
        amount: Int,
        checkAvailability: Boolean
    ): Result<ShopTransaction> = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        if (isDirty) saveWithoutLock()

        val balance = vaultHook?.economy!!.getBalance(player).toBigDecimal()
        val cost = shopItem.price * amount.toBigDecimal()
        val ticketConsumption = shopItem.ticketConsumption * amount

        if (!player.isOnline) {
            return Result.failure(ShopTransactionException.PlayerOffline(this))
        }
        if (checkAvailability && shopItem.availableDays.isNotEmpty() && LocalDateTime.now().dayOfWeek !in shopItem.availableDays) {
            return Result.failure(ShopTransactionException.ShopItemNotAvailable(this, shopItem))
        }
        if (ticket < ticketConsumption) {
            return Result.failure(ShopTransactionException.TicketNotEnough(this, ticketConsumption))
        }
        if (balance < cost) {
            return Result.failure(ShopTransactionException.BalanceNotEnough(this, cost))
        }
        val ticketAfterTransaction = ticket - ticketConsumption

        val transactionModel = TransactionModel(
            id = UUID.randomUUID(),
            playerId = uniqueId,
            time = Instant.now(),
            shopItemId = shopItem.id,
            itemTypeString = shopItem.itemStack.type.asItemType()?.key().toString(),
            itemStackBinary = BsonBinary(shopItem.itemStack.serializeAsBytes()),
            amount = amount,
            quantity = shopItem.quantity * amount,
            ticket = ticketConsumption,
            cost = cost,
            balance = balance - cost,
        )
        val transaction = ShopTransactionImpl(transactionModel)

        val databaseResult = MongoConnection.withTransaction {
            userRepo.update(it, uniqueId, Updates.set("ticket", ticketAfterTransaction))
            transactionRepo.insert(it, transactionModel)
        }

        if (databaseResult.isFailure) {
            val e = databaseResult.exceptionOrNull()
                ?: IllegalStateException("Database operation failed without exception")
            return Result.failure(ShopTransactionException.DatabaseFailure(this, e))
        }

        _ticket -= ticketConsumption
        vaultHook?.economy!!.withdrawPlayer(player, cost.toDouble())
        updateTicketRecoverySchedule()

        withSync {
            repeat(amount) {
                player.player?.inventory?.addItemOrDrop(shopItem.itemStack)
            }
        }

        return Result.success(transaction)
    }

    override suspend fun batchTransaction(purchases: Map<ShopItem, ShopTransactionParameters>): Map<ShopItem, Result<ShopTransaction>> {
        check(isValid) { "Instance not valid" }
        return purchases.entries.associate { (shopItemId, parameters) ->
            shopItemId to makeTransaction(shopItemId, parameters.amount, parameters.checkAvailability)
        }
    }

    override suspend fun save() = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        saveWithoutLock()
    }

    private suspend fun saveWithoutLock() = withContext(Dispatchers.IO) {
        check(isValid) { "Instance not valid" }
        if (!isDirty) return@withContext
        val updates = Updates.combine(
            Updates.set("ticket", ticket),
            Updates.set("lastTicketRecoveryTime", lastTicketRecoveryTime),
        )
        userRepo.update(uniqueId, updates)
        isDirty = false
    }

    override suspend fun close(): Unit = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        unscheduleTicketRecovery()
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
        isValid = false
    }
}
