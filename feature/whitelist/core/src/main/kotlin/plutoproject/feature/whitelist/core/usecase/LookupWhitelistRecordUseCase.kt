package plutoproject.feature.whitelist.core.usecase

import plutoproject.feature.whitelist.core.WhitelistRecord
import plutoproject.feature.whitelist.core.WhitelistRecordRepository
import java.util.UUID

class LookupWhitelistRecordUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
) {
    suspend fun execute(uniqueId: UUID): WhitelistRecord? {
        return whitelistRecords.findByUniqueId(uniqueId)
    }
}
