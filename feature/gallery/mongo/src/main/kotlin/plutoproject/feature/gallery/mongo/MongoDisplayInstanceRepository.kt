package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.infra.mongo.model.DisplayInstanceDocument
import java.util.UUID

class MongoDisplayInstanceRepository(
    private val collection: MongoCollection<DisplayInstanceDocument>,
) : DisplayInstanceRepository {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun ensureIndexes() {
        collection.createIndex(
            Indexes.ascending(DisplayInstanceDocument::id.name),
            IndexOptions().unique(true),
        )
        collection.createIndex(Indexes.ascending(DisplayInstanceDocument::imageId.name))
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending(DisplayInstanceDocument::chunkX.name),
                Indexes.ascending(DisplayInstanceDocument::chunkZ.name),
            )
        )
    }

    override suspend fun findById(id: UUID): DisplayInstance? {
        return collection.find(eq(DisplayInstanceDocument::id.name, id))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByIds(ids: Collection<UUID>): Map<UUID, DisplayInstance> {
        if (ids.isEmpty()) {
            return emptyMap()
        }

        return collection.find(`in`(DisplayInstanceDocument::id.name, ids))
            .toList()
            .map { it.toDomain() }
            .associateBy(DisplayInstance::id)
    }

    override suspend fun findByImageId(imageId: UUID): List<DisplayInstance> {
        return collection.find(eq(DisplayInstanceDocument::imageId.name, imageId))
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
