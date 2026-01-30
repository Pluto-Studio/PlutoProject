package plutoproject.feature.common.whitelist_v2

import plutoproject.feature.common.api.whitelist_v2.WhitelistOperator
import plutoproject.feature.common.api.whitelist_v2.WhitelistRecord
import plutoproject.feature.common.api.whitelist_v2.WhitelistRevokeReason
import java.util.*
import kotlin.time.Instant

class WhitelistRecordImpl(
    override val uniqueId: UUID,
    override val username: String,
    override val granter: WhitelistOperator,
    override val createdAt: Instant,
    override val joinedAsVisitorBefore: Boolean,
    override val isMigrated: Boolean,
    override val isRevoked: Boolean,
    override val revoker: WhitelistOperator?,
    override val revokeReason: WhitelistRevokeReason?
) : WhitelistRecord
