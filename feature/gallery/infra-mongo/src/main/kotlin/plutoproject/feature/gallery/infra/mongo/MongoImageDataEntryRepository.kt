package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import java.util.UUID

class MongoImageDataEntryRepository(
    private val collection: MongoCollection<ImageDataEntryDocument>,
) : ImageDataEntryRepository {
    private val upsert = ReplaceOptions().upsert(true)

    override suspend fun findByBelongsTo(belongsTo: UUID): ImageDataEntry<*>? {
        return collection.find(eq(ImageDataEntryDocument::belongsTo.name, belongsTo))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun save(entry: ImageDataEntry<*>) {
        val document = entry.toDocument()
        collection.replaceOne(
            eq(ImageDataEntryDocument::belongsTo.name, document.belongsTo),
            document,
            upsert,
        )
    }

    override suspend fun deleteByBelongsTo(belongsTo: UUID) {
        collection.deleteOne(eq(ImageDataEntryDocument::belongsTo.name, belongsTo))
    }
}
