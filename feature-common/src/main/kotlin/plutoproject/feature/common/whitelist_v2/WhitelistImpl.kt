package plutoproject.feature.common.whitelist_v2

import plutoproject.feature.common.api.whitelist_v2.VisitorRecord
import plutoproject.feature.common.api.whitelist_v2.VisitorRecordParams
import plutoproject.feature.common.api.whitelist_v2.Whitelist
import plutoproject.feature.common.api.whitelist_v2.WhitelistOperator
import plutoproject.feature.common.api.whitelist_v2.WhitelistRecord
import plutoproject.feature.common.api.whitelist_v2.WhitelistRevokeReason
import java.util.UUID

class WhitelistImpl : Whitelist {
    override suspend fun isWhitelisted(uniqueId: UUID): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun lookupWhitelistRecord(uniqueId: UUID): WhitelistRecord? {
        TODO("Not yet implemented")
    }

    override suspend fun grantWhitelist(
        uniqueId: UUID,
        operator: WhitelistOperator
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun revokeWhitelist(
        uniqueId: UUID,
        operator: WhitelistOperator,
        reason: WhitelistRevokeReason
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun isKnownVisitor(uniqueId: UUID): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun lookupVisitorRecord(uniqueId: UUID): List<VisitorRecord> {
        TODO("Not yet implemented")
    }

    override suspend fun createVisitorRecord(
        uniqueId: UUID,
        params: VisitorRecordParams
    ): VisitorRecord {
        TODO("Not yet implemented")
    }
}
