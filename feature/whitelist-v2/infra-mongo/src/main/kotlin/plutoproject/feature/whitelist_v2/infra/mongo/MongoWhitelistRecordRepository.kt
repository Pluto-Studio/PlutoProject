package plutoproject.feature.whitelist_v2.infra.mongo

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import plutoproject.feature.whitelist_v2.core.WhitelistRecord
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.model.WhitelistRecordDocument
import java.util.UUID

class MongoWhitelistRecordRepository(
    private val collection: MongoCollection<WhitelistRecordDocument>,
) : WhitelistRecordRepository {
    private val upsert = ReplaceOptions().upsert(true)

    override suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecord? {
        return collection.find(eq(WhitelistRecordDocument::uniqueId.name, uniqueId))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecord? {
        return collection.find(eq(WhitelistRecordDocument::uniqueId.name, uniqueId))
            .firstOrNull()
            ?.takeIf { !it.isRevoked }
            ?.toDomain()
    }

    override suspend fun hasActiveByUniqueId(uniqueId: UUID): Boolean {
        return findActiveByUniqueId(uniqueId) != null
    }

    override suspend fun count(): Long {
        return collection.countDocuments()
    }

    override suspend fun countActive(): Long {
        return collection.countDocuments(eq(WhitelistRecordDocument::isRevoked.name, false))
    }

    override suspend fun insertAll(records: List<WhitelistRecord>) {
        if (records.isEmpty()) return
        collection.insertMany(records.map { it.toDocument() })
    }

    override suspend fun saveOrUpdate(record: WhitelistRecord) {
        val document = record.toDocument()
        collection.replaceOne(eq(WhitelistRecordDocument::uniqueId.name, document.uniqueId), document, upsert)
    }
}
