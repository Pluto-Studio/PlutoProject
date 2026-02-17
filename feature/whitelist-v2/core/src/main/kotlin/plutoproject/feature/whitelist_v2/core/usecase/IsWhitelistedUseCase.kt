package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import java.util.UUID

class IsWhitelistedUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
) {
    suspend fun execute(uniqueId: UUID): Boolean {
        return whitelistRecords.hasActiveByUniqueId(uniqueId)
    }
}
