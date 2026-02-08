package plutoproject.feature.whitelist_v2.application

import java.util.UUID

interface WhitelistRecordRepository {
    suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecordData?

    suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecordData?

    suspend fun hasActiveByUniqueId(uniqueId: UUID): Boolean

    suspend fun count(): Long

    suspend fun countActive(): Long

    suspend fun insertAll(records: List<WhitelistRecordData>)

    suspend fun saveOrUpdate(record: WhitelistRecordData)
}
