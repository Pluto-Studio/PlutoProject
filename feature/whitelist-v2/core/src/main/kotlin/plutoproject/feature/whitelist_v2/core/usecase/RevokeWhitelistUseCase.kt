package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.WhitelistOperator
import plutoproject.feature.whitelist_v2.core.WhitelistRevokeReason
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import java.time.Clock
import java.util.UUID

class RevokeWhitelistUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
    private val clock: Clock,
) {
    suspend fun execute(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Boolean {
        val record = whitelistRecords.findActiveByUniqueId(uniqueId) ?: return false

        whitelistRecords.saveOrUpdate(
            record.copy(
                isRevoked = true,
                revoker = operator,
                revokeReason = reason,
                revokeAt = clock.instant(),
            )
        )
        return true
    }
}
