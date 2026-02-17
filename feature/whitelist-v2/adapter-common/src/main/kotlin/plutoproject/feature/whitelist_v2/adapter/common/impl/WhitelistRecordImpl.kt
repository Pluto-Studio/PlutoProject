package plutoproject.feature.whitelist_v2.adapter.common.impl

import plutoproject.feature.whitelist_v2.api.WhitelistOperator
import plutoproject.feature.whitelist_v2.api.WhitelistRecord
import plutoproject.feature.whitelist_v2.api.WhitelistRevokeReason
import plutoproject.feature.whitelist_v2.core.WhitelistRecordData
import java.time.Instant
import java.util.*

class WhitelistRecordImpl(
    private val data: WhitelistRecordData,
) : WhitelistRecord {
    override val uniqueId: UUID
        get() = data.uniqueId

    override val username: String
        get() = data.username

    override val granter: WhitelistOperator
        get() = data.granter.toApi()

    override val createdAt: Instant
        get() = data.createdAt

    override val joinedAsVisitorBefore: Boolean
        get() = data.joinedAsVisitorBefore

    override val isMigrated: Boolean
        get() = data.isMigrated

    override val isRevoked: Boolean
        get() = data.isRevoked

    override val revoker: WhitelistOperator?
        get() = data.revoker?.toApi()

    override val revokeReason: WhitelistRevokeReason?
        get() = data.revokeReason?.toApi()

    override val revokeAt: Instant?
        get() = data.revokeAt
}
