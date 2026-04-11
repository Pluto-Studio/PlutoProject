package plutoproject.feature.paper.daily

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import plutoproject.feature.paper.api.daily.DailyHistory
import plutoproject.feature.paper.daily.models.DailyHistoryModel
import plutoproject.framework.common.util.data.uuid
import plutoproject.framework.common.util.time.LocalZoneId
import plutoproject.framework.common.util.time.toInstant
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
