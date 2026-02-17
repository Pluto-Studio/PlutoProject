package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import plutoproject.feature.whitelist_v2.core.WhitelistOperator
import plutoproject.feature.whitelist_v2.core.WhitelistRecordData
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import java.time.Clock
import java.util.*

class GrantWhitelistUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
    private val visitorRecords: VisitorRecordRepository,
    private val clock: Clock,
) {
    sealed class Result {
        object Ok : Result()
        object AlreadyGranted : Result()
    }

    suspend fun execute(uniqueId: UUID, username: String, operator: WhitelistOperator): Result {
        if (whitelistRecords.hasActiveByUniqueId(uniqueId)) {
            return Result.AlreadyGranted
        }

        val hasVisitorRecord = visitorRecords.hasByUniqueId(uniqueId)
        val existing = whitelistRecords.findByUniqueId(uniqueId)

        val record = existing?.copy(
            username = username,
            granter = operator,
            joinedAsVisitorBefore = hasVisitorRecord,
            isRevoked = false,
            revoker = null,
            revokeReason = null,
            revokeAt = null,
        ) ?: WhitelistRecordData(
            uniqueId = uniqueId,
            username = username,
            granter = operator,
            createdAt = clock.instant(),
            joinedAsVisitorBefore = hasVisitorRecord,
            isMigrated = false,
            isRevoked = false,
            revoker = null,
            revokeReason = null,
            revokeAt = null,
        )

        whitelistRecords.saveOrUpdate(record)
        return Result.Ok
    }
}
