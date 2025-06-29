package plutoproject.feature.paper.daily

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import plutoproject.feature.paper.api.daily.Daily
import plutoproject.feature.paper.api.daily.DailyHistory
import plutoproject.feature.paper.api.daily.DailyUser
import plutoproject.feature.paper.daily.models.DailyHistoryModel
import plutoproject.feature.paper.daily.models.DailyUserModel
import plutoproject.feature.paper.daily.models.toModel
import plutoproject.feature.paper.daily.repositories.DailyHistoryRepository
import plutoproject.feature.paper.daily.repositories.DailyUserRepository
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.common.util.time.LocalZoneId
import plutoproject.framework.common.util.time.currentTimestampMillis
import plutoproject.framework.common.util.time.toInstant
import plutoproject.framework.common.util.trimmedString
import plutoproject.framework.paper.util.hook.vaultHook
import java.time.Instant
import java.time.LocalDate
import java.util.*

class DailyUserImpl(model: DailyUserModel) : DailyUser, KoinComponent {
    private val rewardConfig by lazy { get<DailyConfig>().rewards }
    private val historyRepo by inject<DailyHistoryRepository>()
    private val userRepo by inject<DailyUserRepository>()

    override val id: UUID = model.id.convertToUuid()
    override val player: OfflinePlayer by lazy { Bukkit.getOfflinePlayer(id) }
    override var lastCheckIn: Instant? = model.lastCheckIn?.toInstant()
    override val lastCheckInDate: LocalDate?
        get() = lastCheckIn?.let { LocalDate.ofInstant(lastCheckIn, LocalZoneId) }
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
            createdAt = currentTimestampMillis,
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
        vaultHook?.economy?.depositPlayer(player, reward)
        player.player?.sendMessage(COIN_CLAIMED.replace("<amount>", reward.trimmedString()))
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
