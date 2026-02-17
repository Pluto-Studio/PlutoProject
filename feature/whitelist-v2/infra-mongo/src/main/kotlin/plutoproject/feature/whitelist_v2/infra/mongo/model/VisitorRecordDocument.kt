package plutoproject.feature.whitelist_v2.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.framework.common.util.data.serializers.bson.InstantAsBsonDateTimeSerializer
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.time.Instant
import java.util.UUID

@Serializable
data class VisitorRecordDocument(
    val uniqueId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val ipAddress: IpAddressDocument,
    val virtualHost: String,
    val visitedAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant,
    val createdAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant = Instant.now(),
    val durationMillis: Long,
    val visitedServers: Collection<String>,
)
