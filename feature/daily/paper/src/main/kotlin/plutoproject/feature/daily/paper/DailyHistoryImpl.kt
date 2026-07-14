package plutoproject.feature.daily.paper

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import plutoproject.feature.daily.api.paper.DailyHistory
import plutoproject.feature.daily.paper.models.DailyHistoryModel
import plutoproject.foundation.common.serialization.uuid
import plutoproject.foundation.common.time.LocalZoneId
import plutoproject.foundation.common.time.toInstant
import java.time.Instant
import java.time.LocalDate
import java.util.*

class DailyHistoryImpl(model: DailyHistoryModel) : DailyHistory {
    override val id: UUID = model.id.uuid()
    override val owner: UUID = model.owner.uuid()
    override val ownerPlayer: OfflinePlayer by lazy { Bukkit.getOfflinePlayer(owner) }
    override val createdAt: Instant = model.createdAt.toInstant()
    override val createdDate: LocalDate = LocalDate.ofInstant(createdAt, LocalZoneId)
    override val rewarded: Double = model.rewarded
}
