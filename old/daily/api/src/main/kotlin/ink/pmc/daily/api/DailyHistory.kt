package ink.pmc.daily.api

import org.bukkit.OfflinePlayer
import java.time.Instant
import java.time.LocalDate
import java.util.*

interface DailyHistory {

    val id: UUID
    val owner: UUID
    val ownerPlayer: OfflinePlayer
    val createdAt: Instant
    val createdDate: LocalDate
    val rewarded: Double

}