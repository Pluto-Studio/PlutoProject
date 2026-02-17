package plutoproject.feature.whitelist_v2.core

import java.time.Instant
import java.util.UUID

data class WhitelistRecordData(
    val uniqueId: UUID,
    val username: String,
    val granter: WhitelistOperator,
    val createdAt: Instant,
    val joinedAsVisitorBefore: Boolean,
    val isMigrated: Boolean,
    val isRevoked: Boolean,
    val revoker: WhitelistOperator?,
    val revokeReason: WhitelistRevokeReason?,
    val revokeAt: Instant?,
)
