package plutoproject.feature.whitelist_v2.core.usecase

import plutoproject.feature.whitelist_v2.core.WhitelistRecordData
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import java.util.UUID

class LookupWhitelistRecordUseCase(
    private val whitelistRecords: WhitelistRecordRepository,
) {
    suspend fun execute(uniqueId: UUID): WhitelistRecordData? {
        return whitelistRecords.findByUniqueId(uniqueId)
    }
}
