package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.WhitelistRecord
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import java.util.UUID

class LookupWhitelistRecordUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
) {
    suspend fun execute(uniqueId: UUID): WhitelistRecord? {
        return whitelistRecords.findByUniqueId(uniqueId)
    }
}
