package plutoproject.feature.paper.exchangeshop.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.InstantAsBsonDateTimeSerializer
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.time.Instant
import java.util.*

@Serializable
data class UserModel(
    @SerialName("_id") val uniqueId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val createTime: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant,
    val ticket: Int,
    val lastTicketRecoveryTime: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant?,
)
