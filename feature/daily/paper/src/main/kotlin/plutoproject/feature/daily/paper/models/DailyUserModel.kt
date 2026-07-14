package plutoproject.feature.daily.paper.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import plutoproject.feature.daily.api.paper.DailyUser

internal fun DailyUser.toModel(): DailyUserModel {
    return DailyUserModel(
        id = id.toString(),
        lastCheckIn = lastCheckIn?.toEpochMilli(),
        accumulatedDays = accumulatedDays,
    )
}

@Serializable
data class DailyUserModel(
    @SerialName("_id") val id: String,
    val lastCheckIn: Long?,
    val accumulatedDays: Int,
)
