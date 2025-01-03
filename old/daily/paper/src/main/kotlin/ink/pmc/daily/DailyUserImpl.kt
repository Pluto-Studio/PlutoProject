package ink.pmc.daily

import ink.pmc.daily.api.Daily
import ink.pmc.daily.api.DailyHistory
import ink.pmc.daily.api.DailyUser
import ink.pmc.daily.models.DailyHistoryModel
import ink.pmc.daily.models.DailyUserModel
import ink.pmc.daily.models.toModel
import ink.pmc.daily.repositories.DailyHistoryRepository
import ink.pmc.daily.repositories.DailyUserRepository
import ink.pmc.framework.chat.replace
import ink.pmc.framework.currentUnixTimestamp
import ink.pmc.framework.player.uuid
import ink.pmc.framework.time.currentZoneId
import ink.pmc.framework.time.instant
import ink.pmc.framework.trimmed
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.util.*

class DailyUserImpl(model: DailyUserModel) : DailyUser, KoinComponent {
    private val rewardConfig by lazy { get<DailyConfig>().rewards }
    private val historyRepo by inject<DailyHistoryRepository>()
    private val userRepo by inject<DailyUserRepository>()

    override val id: UUID = model.id.uuid
    override val player: OfflinePlayer by lazy { Bukkit.getOfflinePlayer(id) }
    override var lastCheckIn: Instant? = model.lastCheckIn?.instant
    override val lastCheckInDate: LocalDate?
        get() = lastCheckIn?.let { LocalDate.ofInstant(lastCheckIn, currentZoneId) }
    override var accumulatedDays: Int = model.accumulatedDays

    override suspend fun checkIn(): DailyHistory {
        require(!isCheckedInToday()) { "User $id already checked-in today" }
        checkCheckInDate()
        if (lastCheckInDate?.month != LocalDate.now().month || !isCheckedInYesterday()) {
            accumulatedDays = 0
        }
        val reward = getReward()
        val history = DailyHistoryModel(
            owner = id.toString(),
            createdAt = currentUnixTimestamp,
            rewarded = reward,
        )
        lastCheckIn = Instant.now()
        accumulatedDays++
        historyRepo.saveOrUpdate(history)
        update()
        player.player?.sendMessage(CHECK_IN.replace("<acc>", accumulatedDays))
        performReward(reward)
        return DailyHistoryImpl(history).also { Daily.loadHistory(it) }
    }

    private fun performReward(reward: Double) {
        economy.depositPlayer(player, reward)
        player.player?.sendMessage(COIN_CLAIM.replace("<amount>", reward.trimmed()))
    }

    override suspend fun clearAccumulation() {
        accumulatedDays = 0
        update()
    }

    override suspend fun resetCheckInTime() {
        lastCheckIn = null
        update()
    }

    override suspend fun isCheckedInToday(): Boolean {
        return Daily.getHistoryByTime(id, LocalDate.now()) != null
    }

    override suspend fun isCheckedInYesterday(): Boolean {
        val yesterday = LocalDate.now().minusDays(1)
        return Daily.getHistoryByTime(id, yesterday) != null
    }

    override fun getReward(): Double {
        val date = LocalDate.now()
        val base = if (date.dayOfWeek.value in 1..5) rewardConfig.weekday else rewardConfig.weekend
        val accumulate = if (accumulatedDays > 0 && accumulatedDays % rewardConfig.accumulateRequirement == 0)
            rewardConfig.accumulate else 0.0
        return base + accumulate
    }

    override suspend fun update() {
        userRepo.saveOrUpdate(toModel())
    }
}