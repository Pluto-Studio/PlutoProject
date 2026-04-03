package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDataEntryDocument
import java.util.UUID

class MongoImageDataEntryRepository(
    private val collection: MongoCollection<ImageDataEntryDocument>,
) : ImageDataEntryRepository {
    private val upsert = ReplaceOptions().upsert(true)

    override suspend fun findByImageId(imageId: UUID): ImageDataEntry<*>? {
        return collection.find(eq(ImageDataEntryDocument::imageId.name, imageId))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByImageIds(imageIds: Collection<UUID>): Map<UUID, ImageDataEntry<*>> {
        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        return collection.find(`in`(ImageDataEntryDocument::imageId.name, imageIds))
            .toList()
            .map { it.toDomain() }
            .associateBy(ImageDataEntry<*>::imageId)
    }

    override suspend fun save(entry: ImageDataEntry<*>) {
        val document = entry.toDocument()
        collection.replaceOne(
            eq(ImageDataEntryDocument::imageId.name, document.imageId),
            document,
            upsert,
        )
    }

    override suspend fun deleteByImageId(belongsTo: UUID) {
        collection.deleteOne(eq(ImageDataEntryDocument::imageId.name, belongsTo))
    }
}
