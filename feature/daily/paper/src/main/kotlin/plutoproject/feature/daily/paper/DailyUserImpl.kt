package plutoproject.feature.daily.paper

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import plutoproject.kernel.api.koinInject
import plutoproject.feature.daily.api.paper.Daily
import plutoproject.feature.daily.api.paper.DailyHistory
import plutoproject.feature.daily.api.paper.DailyUser
import plutoproject.feature.daily.paper.models.DailyHistoryModel
import plutoproject.feature.daily.paper.models.DailyUserModel
import plutoproject.feature.daily.paper.models.toModel
import plutoproject.feature.daily.paper.repositories.DailyHistoryRepository
import plutoproject.feature.daily.paper.repositories.DailyUserRepository
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.serialization.uuid
import plutoproject.foundation.common.time.LocalZoneId
import plutoproject.foundation.common.time.currentTimestampMillis
import plutoproject.foundation.common.time.toInstant
import plutoproject.foundation.common.text.trimmedString
import plutoproject.foundation.paper.hook.vaultEconomy
import java.time.Instant
import java.time.LocalDate
import java.util.*

class DailyUserImpl(model: DailyUserModel) : DailyUser {
    private val rewardConfig by lazy { plutoproject.kernel.api.koinGet<DailyConfig>().rewards }
    private val historyRepo by koinInject<DailyHistoryRepository>()
    private val userRepo by koinInject<DailyUserRepository>()

    override val id: UUID = model.id.uuid()
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
        return DailyHistoryImpl(history).also { plutoproject.kernel.api.koinGet<Daily>().loadHistory(it) }
    }

    private fun performReward(reward: Double) {
        server.vaultEconomy?.depositPlayer(player, reward)
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
        return plutoproject.kernel.api.koinGet<Daily>().getHistoryByTime(id, LocalDate.now()) != null
    }

    override suspend fun isCheckedInYesterday(): Boolean {
        val yesterday = LocalDate.now().minusDays(1)
        return plutoproject.kernel.api.koinGet<Daily>().getHistoryByTime(id, yesterday) != null
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
