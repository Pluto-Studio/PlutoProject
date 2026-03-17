package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.gallery.core.DisplayInstance
import plutoproject.feature.gallery.core.DisplayInstanceRepository
import plutoproject.feature.gallery.infra.mongo.model.DisplayInstanceDocument
import java.util.UUID

class MongoDisplayInstanceRepository(
    private val collection: MongoCollection<DisplayInstanceDocument>,
) : DisplayInstanceRepository {
    private val upsert = ReplaceOptions().upsert(true)

    override suspend fun findById(id: UUID): DisplayInstance? {
        return collection.find(eq(DisplayInstanceDocument::id.name, id))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByBelongsTo(belongsTo: UUID): List<DisplayInstance> {
        return collection.find(eq(DisplayInstanceDocument::belongsTo.name, belongsTo))
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun findByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance> {
        return collection.find(
            and(
                eq(DisplayInstanceDocument::chunkX.name, chunkX),
                eq(DisplayInstanceDocument::chunkZ.name, chunkZ),
            )
        )
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun save(displayInstance: DisplayInstance) {
        val document = displayInstance.toDocument()
        collection.replaceOne(eq(DisplayInstanceDocument::id.name, displayInstance.id), document, upsert)
    }

    override suspend fun deleteById(id: UUID) {
        collection.deleteOne(eq(DisplayInstanceDocument::id.name, id))
    }
}
