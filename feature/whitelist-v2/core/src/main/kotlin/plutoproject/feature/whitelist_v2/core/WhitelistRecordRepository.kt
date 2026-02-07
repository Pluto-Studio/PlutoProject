package plutoproject.feature.whitelist_v2.core

import java.util.UUID

interface WhitelistRecordRepository {
    suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecordData?

    suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecordData?

    suspend fun hasActiveByUniqueId(uniqueId: UUID): Boolean

    suspend fun saveOrUpdate(record: WhitelistRecordData)
}
