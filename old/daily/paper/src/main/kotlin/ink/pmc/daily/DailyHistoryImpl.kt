package ink.pmc.daily

import ink.pmc.daily.api.DailyHistory
import ink.pmc.daily.models.DailyHistoryModel
import ink.pmc.framework.player.uuid
import ink.pmc.framework.time.currentZoneId
import ink.pmc.framework.time.instant
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.time.Instant
import java.time.LocalDate
import java.util.*

class DailyHistoryImpl(model: DailyHistoryModel) : DailyHistory {

    override val id: UUID = model.id.uuid
    override val owner: UUID = model.owner.uuid
    override val ownerPlayer: OfflinePlayer by lazy { Bukkit.getOfflinePlayer(owner) }
    override val createdAt: Instant = model.createdAt.instant
    override val createdDate: LocalDate = LocalDate.ofInstant(createdAt, currentZoneId)
    override val rewarded: Double = model.rewarded

}