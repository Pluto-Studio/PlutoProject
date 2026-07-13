package plutoproject.feature.whitelist.core.usecase

import plutoproject.feature.whitelist.core.WhitelistRecordRepository
import java.util.UUID

class IsWhitelistedUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
) {
    suspend fun execute(uniqueId: UUID): Boolean {
        return whitelistRecords.hasActiveByUniqueId(uniqueId)
    }
}
