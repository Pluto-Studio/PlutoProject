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
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.hook.vaultHook
import plutoproject.framework.paper.util.inventory.addItemOrDrop
import plutoproject.framework.paper.util.server
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class ShopUserImpl(
    override val uniqueId: UUID,
    override val player: OfflinePlayer,
    override val createdAt: Instant,
    ticket: Long,
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
        scheduledTicketRecoveryTime = model.scheduledTicketRecoveryTime,
    )

    override var isValid by MutableStateFlow(true)
    override var ticket: Long
        get() = _ticket
        set(value) = coroutineScope.launch { setTicket(value) }.asCompletableFuture().join()
    override val fullTicketRecoveryTime: Instant?
        get() {
            if (ticket >= config.ticket.recoveryCap) return null
            if (scheduledTicketRecoveryTime == null) return null
            val currentTime = Instant.now()
            val intervalUntilSchedule = Duration.between(currentTime, scheduledTicketRecoveryTime).toMillis()
            val unrecoveredAmount = config.ticket.recoveryCap - (ticket + 1) // 用来计算下次恢复时间到回满的间隔，所以要把还没恢复的这个加上

            // 这个时间由两段组成：现在到下次恢复的间隔 + 下次恢复到回满的间隔
            return currentTime.plusMillis(intervalUntilSchedule)
                .plusMillis(unrecoveredAmount * config.ticket.recoveryInterval.toMillis())
        }

    init {
        if (config.ticket.naturalRecovery) {
            coroutineScope.launch { performOfflineRecovery() }
        }
    }

    private suspend fun performOfflineRecovery() = ticketLock.withLock {
        if (scheduledTicketRecoveryTime == null) return
        val currentTime = Instant.now()

        // 已计划的恢复还没有截止，继续它
        if (scheduledTicketRecoveryTime!!.isAfter(currentTime)) {
            scheduleTicketRecovery(scheduledTicketRecoveryTime!!)
            return
        }

        // 已计划的恢复已经截止，计算剩余离线恢复量
        val elapsed = Duration.between(scheduledTicketRecoveryTime, currentTime).toMillis()
        val intervals = elapsed / config.ticket.recoveryInterval.toMillis() + 1 // 截止的恢复任务需要额外加一
        val amount = _ticket + intervals * config.ticket.recoveryAmount
        _ticket = amount.coerceIn(0, config.ticket.recoveryCap)
        scheduledTicketRecoveryTime = null
        markDirtyAndSave()

        if (_ticket >= config.ticket.recoveryCap) return

        // 还没回满，计算到下次恢复已经经过的时长并开启新计划
        val passedMillis = elapsed % config.ticket.recoveryInterval.toMillis()
        val intervalUntilNextRecovery = config.ticket.recoveryInterval.toMillis() - passedMillis
        val nextRecoveryTime = currentTime.plusMillis(intervalUntilNextRecovery)
        scheduleTicketRecovery(nextRecoveryTime)
    }

    private fun recoveryTicket(amount: Long): Boolean {
        if (ticket >= config.ticket.recoveryCap) return false
        _ticket = (_ticket + amount).coerceIn(0, config.ticket.recoveryCap)
        return true
    }

    // 计算从现在开始下次恢复的时间
    private fun calculateNextRecoveryTime(): Instant {
        val currentTime = Instant.now()
        return currentTime.plusMillis(config.ticket.recoveryInterval.toMillis())
    }

    private suspend fun scheduleTicketRecovery(time: Instant) {
        check(scheduledTicketRecovery == null) { "There is a scheduled ticket recovery unfinished" }
        featureLogger.info("Scheduling ticket recovery for ${player.name} (isValid = $isValid, isActive = ${coroutineScope.isActive})")
        if (!isValid) return
        if (!config.ticket.naturalRecovery) return
        if (ticket >= config.ticket.recoveryCap) return

        val currentTime = Instant.now()
        val interval = Duration.between(currentTime, time).toMillis()

        check(time.isAfter(currentTime)) { "Ticket recovery must be scheduled in the future" }

        // 如果只是继续未截止的任务这个值不会变
        if (scheduledTicketRecoveryTime != time) {
            scheduledTicketRecoveryTime = time
            markDirtyAndSave()
        }

        scheduledTicketRecovery = coroutineScope.launch {
            delay(interval)
            ticketLock.withLock { performScheduledRecovery() }
        }
        featureLogger.info("Scheduled ticket recovery for ${player.name} on $time")
    }

    private suspend fun performScheduledRecovery() {
        recoveryTicket(config.ticket.recoveryAmount)
        scheduledTicketRecovery = null
        scheduledTicketRecoveryTime = null
        markDirtyAndSave()
        // 还没有恢复满，继续计划恢复
        if (ticket < config.ticket.recoveryCap) {
            if (!coroutineScope.isActive || !isValid) return
            scheduleTicketRecovery(calculateNextRecoveryTime())
        }
    }

    private suspend fun unscheduleTicketRecovery(keepTimeInDatabase: Boolean = false) {
        if (scheduledTicketRecovery == null) return
        scheduledTicketRecovery?.cancelAndJoin()
        scheduledTicketRecovery = null
        if (!keepTimeInDatabase) {
            scheduledTicketRecoveryTime = null
            markDirtyAndSave()
        }
        featureLogger.info("Unscheduled ticket recovery for ${player.name}")
    }

    private suspend fun updateTicketRecoverySchedule() {
        if (scheduledTicketRecovery != null && ticket >= config.ticket.recoveryCap) {
            logger.info("Ticket remaining enough, unschedule")
            unscheduleTicketRecovery()
            return
        }
        if (scheduledTicketRecovery == null && config.ticket.naturalRecovery) {
            scheduleTicketRecovery(calculateNextRecoveryTime())
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

    override suspend fun withdrawTicket(amount: Long): Long = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        require(ticket >= amount) { "Insufficient tickets for `$uniqueId`, only $ticket left" }
        _ticket -= amount
        markDirtyAndSave()
        updateTicketRecoverySchedule()
        return _ticket
    }

    override suspend fun depositTicket(amount: Long): Long = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        _ticket += amount
        markDirtyAndSave()
        updateTicketRecoverySchedule()
        return _ticket
    }

    override suspend fun setTicket(amount: Long): Long = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        _ticket = amount
        markDirtyAndSave()
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
            ShopTransactionImpl(model)
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

    private suspend fun markDirtyAndSave() {
        check(isValid) { "Instance not valid" }
        isDirty = true
        saveWithoutLock()
    }

    private suspend fun saveWithoutLock() = withContext(Dispatchers.IO) {
        check(isValid) { "Instance not valid" }
        if (!isDirty) return@withContext
        val updates = Updates.combine(
            Updates.set("ticket", ticket),
            Updates.set("lastTicketRecoveryTime", lastTicketRecoveryTime),
            Updates.set("scheduledTicketRecoveryTime", scheduledTicketRecoveryTime)
        )
        userRepo.update(uniqueId, updates)
        isDirty = false
    }

    override suspend fun close(): Unit = ticketLock.withLock {
        check(isValid) { "Instance not valid" }
        unscheduleTicketRecovery(true)
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
        isValid = false
    }
}
