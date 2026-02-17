package plutoproject.feature.whitelist_v2.core

import java.util.UUID

interface WhitelistRecordRepository {
    suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecord?

    suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecord?

    suspend fun hasActiveByUniqueId(uniqueId: UUID): Boolean

    suspend fun count(): Long

    suspend fun countActive(): Long

    suspend fun insertAll(records: List<WhitelistRecord>)

    suspend fun saveOrUpdate(record: WhitelistRecord)
}
