package plutoproject.feature.common.whitelist_v2.repository

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.common.whitelist_v2.model.WhitelistRecordModel
import java.util.*

class WhitelistRecordRepository(private val collection: MongoCollection<WhitelistRecordModel>) {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun findByUniqueId(uniqueId: UUID): WhitelistRecordModel? {
        return collection.find(eq("uniqueId", uniqueId)).firstOrNull()
    }

    suspend fun findAll(): List<WhitelistRecordModel> {
        return collection.find().toList()
    }

    suspend fun findActiveByUniqueId(uniqueId: UUID): WhitelistRecordModel? {
        return collection.find(
            eq("uniqueId", uniqueId)
        ).firstOrNull()?.takeIf { !it.isRevoked }
    }

    suspend fun hasByUniqueId(uniqueId: UUID): Boolean {
        return findByUniqueId(uniqueId) != null
    }

    suspend fun hasActiveByUniqueId(uniqueId: UUID): Boolean {
        return findActiveByUniqueId(uniqueId) != null
    }

    suspend fun save(model: WhitelistRecordModel) {
        collection.insertOne(model)
    }

    suspend fun update(model: WhitelistRecordModel) {
        collection.replaceOne(eq("uniqueId", model.uniqueId), model, upsert)
    }

    suspend fun deleteByUniqueId(uniqueId: UUID) {
        collection.deleteOne(eq("uniqueId", uniqueId))
    }
}
