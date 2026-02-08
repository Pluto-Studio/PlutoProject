package plutoproject.feature.whitelist_v2.infra.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import plutoproject.framework.common.util.data.serializers.bson.InstantAsBsonDateTimeSerializer
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.time.Instant
import java.util.UUID

@Serializable
data class WhitelistRecordDocument(
    val uniqueId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val username: String,
    val granter: WhitelistOperatorDocument,
    val createdAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant = Instant.now(),
    val joinedAsVisitorBefore: Boolean,
    val isMigrated: Boolean = false,
    val isRevoked: Boolean = false,
    val revoker: WhitelistOperatorDocument? = null,
    val revokeReason: WhitelistRevokeReason? = null,
    val revokeAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant? = null,
)
