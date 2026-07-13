package plutoproject.feature.daily.paper

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.bukkit.OfflinePlayer
import plutoproject.kernel.api.koinInject
import plutoproject.feature.daily.api.paper.Daily
import plutoproject.feature.daily.api.paper.DailyHistory
import plutoproject.feature.daily.api.paper.DailyUser
import plutoproject.feature.daily.paper.models.DailyUserModel
import plutoproject.feature.daily.paper.repositories.DailyHistoryRepository
import plutoproject.feature.daily.paper.repositories.DailyUserRepository
import plutoproject.foundation.common.coroutine.Loom
import plutoproject.feature.daily.paper.moduleScope
import plutoproject.foundation.common.serialization.uuid
import plutoproject.foundation.common.time.LocalZoneId
import plutoproject.foundation.common.time.atEndOfDay
import plutoproject.foundation.common.time.toOffset
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class DailyImpl : Daily {
    private var isShutdown = false
    private val userRepo by koinInject<DailyUserRepository>()
    private val historyRepo by koinInject<DailyHistoryRepository>()
    private val loadedUsers = ConcurrentHashMap<UUID, DailyUser>()
    private val historyCaches = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .buildAsync<UUID, DailyHistory?> { k, _ ->
            moduleScope.async(Dispatchers.Loom) { loadHistory(k) }.asCompletableFuture()
        }

    init {
        moduleScope.launch {
            while (!isShutdown) {
                delay(10.minutes)
                loadedUsers.entries.removeIf { !it.value.player.isOnline }
            }
        }
    }

    private suspend fun loadUser(id: UUID): DailyUser? {
        val model = userRepo.findById(id) ?: return null
        return DailyUserImpl(model)
    }

    private suspend fun loadHistory(id: UUID): DailyHistory? {
        val model = historyRepo.findById(id) ?: return null
        return DailyHistoryImpl(model)
    }

    override suspend fun checkIn(user: UUID): DailyHistory {
        return getUserOrCreate(user).checkIn()
    }

    override suspend fun isCheckedInToday(user: UUID): Boolean {
        return getUser(user)?.isCheckedInToday() ?: false
    }

    override suspend fun createUser(id: UUID): DailyUser {
        require(getUser(id) == null) { "User with id $id already existed" }
        val user = DailyUserModel(
            id = id.toString(),
            lastCheckIn = null,
            accumulatedDays = 0
        )
        userRepo.saveOrUpdate(user)
        return getUser(id) ?: error("Failed to obtain user instance of $id")
    }

    override suspend fun getUserOrCreate(id: UUID): DailyUser {
        return getUser(id) ?: createUser(id)
    }

    override suspend fun getUser(id: UUID): DailyUser? {
        return loadedUsers[id] ?: loadUser(id)?.also { loadedUsers[id] = it }
    }

    override suspend fun getUser(player: OfflinePlayer): DailyUser? {
        return getUser(player.uniqueId)
    }

    override suspend fun getHistory(id: UUID): DailyHistory? {
        return historyCaches.get(id).await()
    }

    override suspend fun listHistory(user: UUID): Collection<DailyHistory> {
        return historyRepo.findByOwner(user).map {
            val uuid = it.id.uuid()
            historyCaches.getIfPresent(uuid)?.get() ?: DailyHistoryImpl(it).also { h ->
                historyCaches.put(uuid, CompletableFuture.completedFuture(h))
            }
        }
    }

    override suspend fun getHistoryByTime(
        user: UUID,
        start: LocalDateTime,
        end: LocalDateTime
    ): Collection<DailyHistory> {
        return getHistoryByTime(
            user,
            start.toInstant(LocalZoneId.toOffset()),
            end.toInstant(LocalZoneId.toOffset())
        )
    }

    override suspend fun getHistoryByTime(user: UUID, start: Instant, end: Instant): Collection<DailyHistory> {
        return getHistoryByTime(user, start.toEpochMilli(), end.toEpochMilli())
    }

    override suspend fun getHistoryByTime(user: UUID, start: Long, end: Long): Collection<DailyHistory> {
        return historyRepo.findByTime(user, start, end).map {
            val uuid = it.id.uuid()
            historyCaches.getIfPresent(uuid)?.get() ?: DailyHistoryImpl(it).also { h ->
                historyCaches.put(uuid, CompletableFuture.completedFuture(h))
            }
        }
    }

    override suspend fun getHistoryByTime(user: UUID, date: LocalDate): DailyHistory? {
        val start = date.atStartOfDay().toInstant(LocalZoneId.toOffset())
        val end = date.atEndOfDay().toInstant(LocalZoneId.toOffset())
        return getHistoryByTime(user, start, end).firstOrNull()
    }

    override suspend fun getAccumulationBetween(
        user: UUID,
        start: LocalDateTime,
        end: LocalDateTime
    ): Int {
        return getAccumulationBetween(
            user,
            start.toInstant(LocalZoneId.toOffset()),
            end.toInstant(LocalZoneId.toOffset())
        )
    }

    override suspend fun getAccumulationBetween(
        user: UUID,
        start: Instant,
        end: Instant
    ): Int {
        return getAccumulationBetween(user, start.toEpochMilli(), end.toEpochMilli())
    }

    override suspend fun getAccumulationBetween(
        user: UUID,
        start: Long,
        end: Long
    ): Int {
        val histories = getHistoryByTime(user, start, end).sortedBy { it.createdDate }
        if (histories.isEmpty()) return 0
        var acc = 1
        var previousDate = histories[0].createdDate
        histories.forEachIndexed { i, e ->
            if (i == 0) return@forEachIndexed
            val curr = e.createdDate
            if (curr.minusDays(1) == previousDate) {
                acc++
            } else {
                acc = 1
            }
            previousDate = curr
        }
        return acc
    }

    override suspend fun getLastCheckIn(user: UUID): Instant? {
        return getUser(user)?.lastCheckIn
    }

    override suspend fun getLastCheckInDate(user: UUID): LocalDate? {
        return getUser(user)?.lastCheckInDate
    }

    override suspend fun getAccumulatedDays(user: UUID): Int {
        return getUser(user)?.accumulatedDays ?: 0
    }

    override fun loadHistory(history: DailyHistory) {
        historyCaches.put(history.id, CompletableFuture.completedFuture(history))
    }

    override fun unloadUser(id: UUID) {
        loadedUsers.remove(id)
    }

    override fun shutdown() {
        require(!isShutdown) { "Daily API already shutdown" }
        isShutdown = true
    }
}
