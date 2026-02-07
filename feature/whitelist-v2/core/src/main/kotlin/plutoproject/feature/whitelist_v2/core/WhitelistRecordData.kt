package plutoproject.feature.whitelist_v2.core

import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRecord
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import java.time.Instant
import java.util.UUID

data class WhitelistRecordData(
    override val uniqueId: UUID,
    override val username: String,
    override val granter: WhitelistOperator,
    override val createdAt: Instant,
    override val joinedAsVisitorBefore: Boolean,
    override val isMigrated: Boolean,
    override val isRevoked: Boolean,
    override val revoker: WhitelistOperator?,
    override val revokeReason: WhitelistRevokeReason?,
    override val revokeAt: Instant?,
) : WhitelistRecord
