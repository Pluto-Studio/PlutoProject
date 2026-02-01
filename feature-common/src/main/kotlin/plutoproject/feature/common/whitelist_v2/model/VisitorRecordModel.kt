package plutoproject.feature.common.whitelist_v2.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.InstantAsBsonDateTimeSerializer
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.time.Instant
import java.util.*

@Serializable
data class VisitorRecordModel(
    val uniqueId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val ipAddress: IpAddressModel,
    val virtualHost: String,
    val visitedAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant,
    val createdAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant = Instant.now(),
    val durationMillis: Long,
    val visitedServers: Collection<String>
)
