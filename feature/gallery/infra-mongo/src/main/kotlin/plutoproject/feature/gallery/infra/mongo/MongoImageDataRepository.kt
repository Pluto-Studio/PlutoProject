package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDataDocument
import java.util.UUID

class MongoImageDataRepository(
    private val collection: MongoCollection<ImageDataDocument>,
) : ImageDataRepository {
    private val upsert = ReplaceOptions().upsert(true)

    suspend fun ensureIndexes() {
        collection.createIndex(
            Indexes.ascending(ImageDataDocument::imageId.name),
            IndexOptions().unique(true),
        )
    }

    override suspend fun findByImageId(imageId: UUID): ImageData? {
        return collection.find(eq(ImageDataDocument::imageId.name, imageId))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByImageIds(imageIds: Collection<UUID>): Map<UUID, ImageData> {
        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        return collection.find(`in`(ImageDataDocument::imageId.name, imageIds))
            .toList()
            .associate { it.imageId to it.toDomain() }
    }

    override suspend fun save(imageId: UUID, data: ImageData) {
        val document = data.toDocument(imageId)
        collection.replaceOne(
            eq(ImageDataDocument::imageId.name, document.imageId),
            document,
            upsert,
        )
    }

    override suspend fun update(imageId: UUID, data: ImageData): Boolean {
        val document = data.toDocument(imageId)
        return collection.replaceOne(
            eq(ImageDataDocument::imageId.name, document.imageId),
            document,
        ).matchedCount > 0
    }

    override suspend fun deleteByImageId(imageId: UUID) {
        collection.deleteOne(eq(ImageDataDocument::imageId.name, imageId))
    }
}
