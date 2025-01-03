package ink.pmc.daily.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.ScreenModel
import ink.pmc.daily.api.Daily
import ink.pmc.daily.api.DailyHistory
import ink.pmc.daily.api.DailyUser
import ink.pmc.framework.time.atEndOfDay
import ink.pmc.framework.time.atStartOfMonth
import ink.pmc.framework.time.toOffset
import ink.pmc.framework.time.zoneId
import org.bukkit.entity.Player
import java.time.LocalDate
import java.time.YearMonth

class DailyCalenderScreenModel(private val player: Player) : ScreenModel {
    val realTime: YearMonth = YearMonth.now()
    var isLoading by mutableStateOf(true)
    var user by mutableStateOf<DailyUser?>(null)
    var yearMonth by mutableStateOf(realTime)
    var accumulatedDays by mutableStateOf(0)
    val loadedHistories = mutableStateListOf<DailyHistory>()
    val checkInDays by derivedStateOf { loadedHistories.size }

    suspend fun init() {
        isLoading = true
        user = Daily.getUserOrCreate(player.uniqueId)
        loadData()
        isLoading = false
    }

    suspend fun loadData() {
        isLoading = true
        val start = yearMonth.atStartOfMonth().atStartOfDay().toInstant(player.zoneId.toOffset())
        val end = yearMonth.atEndOfMonth().atEndOfDay().toInstant(player.zoneId.toOffset())
        accumulatedDays = Daily.getAccumulationBetween(player.uniqueId, start, end)
        loadedHistories.clear()
        loadedHistories.addAll(Daily.getHistoryByTime(player.uniqueId, start, end))
        isLoading = false
    }

    fun getHistory(date: LocalDate): DailyHistory? {
        return loadedHistories.firstOrNull { it.createdDate == date }
    }

    suspend fun goPrevious() {
        yearMonth = yearMonth.minusMonths(1)
        loadData()
    }

    suspend fun goNext() {
        yearMonth = yearMonth.plusMonths(1)
        loadData()
    }

    suspend fun backNow() {
        yearMonth = realTime
        loadData()
    }

    fun canGoPrevious(): Boolean {
        return yearMonth > realTime.minusMonths(12)
    }
}