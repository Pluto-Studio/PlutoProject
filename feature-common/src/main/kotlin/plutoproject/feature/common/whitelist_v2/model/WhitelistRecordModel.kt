package plutoproject.feature.common.whitelist_v2.model

import kotlinx.serialization.Serializable
import plutoproject.feature.common.api.whitelist_v2.WhitelistRevokeReason
import plutoproject.framework.common.util.data.serializers.bson.InstantAsBsonDateTimeSerializer
import plutoproject.framework.common.util.data.serializers.bson.UuidAsBsonBinarySerializer
import java.time.Instant
import java.util.*

@Serializable
data class WhitelistRecordModel(
    val uniqueId: @Serializable(UuidAsBsonBinarySerializer::class) UUID,
    val username: String,
    val granter: WhitelistOperatorModel,
    val createdAt: @Serializable(InstantAsBsonDateTimeSerializer::class) Instant = Instant.now(),
    val joinedAsVisitorBefore: Boolean = false,
    val isMigrated: Boolean = false,
    val isRevoked: Boolean = false,
    val revoker: WhitelistOperatorModel? = null,
    val revokeReason: WhitelistRevokeReason? = null
)
