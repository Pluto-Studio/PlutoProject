package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.WhitelistOperator
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.feature.whitelist_v2.core.WhitelistRevokeReason
import java.time.Clock
import java.util.*

class RevokeWhitelistUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
    private val clock: Clock,
) {
    sealed class Result {
        object Ok : Result()
        object NotGranted : Result()
    }

    suspend fun execute(uniqueId: UUID, operator: WhitelistOperator, reason: WhitelistRevokeReason): Result {
        val record = whitelistRecords.findActiveByUniqueId(uniqueId) ?: return Result.NotGranted

        whitelistRecords.saveOrUpdate(
            record.copy(
                isRevoked = true,
                revoker = operator,
                revokeReason = reason,
                revokeAt = clock.instant(),
            )
        )
        return Result.Ok
    }
}
