package plutoproject.feature.exchangeshop.paper.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import plutoproject.foundation.common.serialization.InstantAsBsonDateTimeSerializer
import plutoproject.foundation.common.bson.UuidAsBsonBinarySerializer
import java.time.Instant
import java.util.*

@Serializable
data class UserModel(
    @SerialName("_id") val uniqueId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val createTime: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant,
    val ticket: Long,
    val lastTicketRecoveryTime: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant?,
    val scheduledTicketRecoveryTime: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant?,
)
