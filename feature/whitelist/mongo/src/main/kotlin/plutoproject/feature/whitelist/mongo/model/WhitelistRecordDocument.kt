package plutoproject.feature.whitelist.mongo.model

import kotlinx.serialization.Serializable
import plutoproject.feature.whitelist.core.WhitelistRevokeReason
import plutoproject.foundation.common.bson.UuidAsBsonBinarySerializer
import plutoproject.foundation.common.serialization.InstantAsBsonDateTimeSerializer
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
