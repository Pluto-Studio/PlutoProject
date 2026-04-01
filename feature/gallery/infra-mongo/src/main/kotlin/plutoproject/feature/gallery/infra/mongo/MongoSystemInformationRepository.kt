package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.setOnInsert
import com.mongodb.kotlin.client.coroutine.MongoCollection
import plutoproject.feature.gallery.core.MapIdRange
import plutoproject.feature.gallery.core.SystemInformationRepository
import plutoproject.feature.gallery.infra.mongo.model.MapIdSystemInformationDocument

private const val MAP_ID_DOCUMENT_ID = "map_id"

class MongoSystemInformationRepository(
    private val mapIdCollection: MongoCollection<MapIdSystemInformationDocument>,
) : SystemInformationRepository {
    override suspend fun allocateMapIds(count: Int, mapIdRange: MapIdRange): Int? {
        val maxLastBeforeAllocate = mapIdRange.end - count
        var initialized = false

        while (true) {
            val updated = mapIdCollection.findOneAndUpdate(
                and(
                    eq(MapIdSystemInformationDocument::id.name, MAP_ID_DOCUMENT_ID),
                    lte(MapIdSystemInformationDocument::lastAllocatedId.name, maxLastBeforeAllocate),
                ),
                inc(MapIdSystemInformationDocument::lastAllocatedId.name, count),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
            if (updated != null) {
                return updated.lastAllocatedId
            }
            if (initialized) {
                return null
            }

            mapIdCollection.updateOne(
                eq(MapIdSystemInformationDocument::id.name, MAP_ID_DOCUMENT_ID),
                setOnInsert(MapIdSystemInformationDocument::lastAllocatedId.name, mapIdRange.start - 1),
                UpdateOptions().upsert(true),
            )
            initialized = true
        }
    }
}
