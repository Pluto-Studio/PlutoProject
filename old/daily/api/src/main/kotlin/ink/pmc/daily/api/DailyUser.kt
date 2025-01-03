package ink.pmc.daily.api

import org.bukkit.OfflinePlayer
import java.time.Instant
import java.time.LocalDate
import java.util.*

interface DailyUser {

    val id: UUID
    val player: OfflinePlayer
    val lastCheckIn: Instant?
    val lastCheckInDate: LocalDate?
    val accumulatedDays: Int

    suspend fun checkIn(): DailyHistory

    suspend fun clearAccumulation()

    suspend fun resetCheckInTime()

    suspend fun isCheckedInToday(): Boolean

    suspend fun isCheckedInYesterday(): Boolean

    fun getReward(): Double

    suspend fun update()

}