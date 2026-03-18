package plutoproject.feature.gallery.infra.mongo

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageRepository
import plutoproject.feature.gallery.infra.mongo.model.ImageDocument
import java.util.UUID

class MongoImageRepository(
    private val collection: MongoCollection<ImageDocument>,
) : ImageRepository {
    private val upsert = ReplaceOptions().upsert(true)

    override suspend fun findById(id: UUID): Image? {
        return collection.find(eq(ImageDocument::id.name, id))
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun findByIds(ids: Collection<UUID>): Map<UUID, Image> {
        if (ids.isEmpty()) {
            return emptyMap()
        }

        return collection.find(`in`(ImageDocument::id.name, ids))
            .toList()
            .map { it.toDomain() }
            .associateBy(Image::id)
    }

    override suspend fun findByOwner(owner: UUID): List<Image> {
        return collection.find(eq(ImageDocument::owner.name, owner))
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun save(image: Image) {
        val document = image.toDocument()
        collection.replaceOne(eq(ImageDocument::id.name, document.id), document, upsert)
    }

    override suspend fun deleteById(id: UUID) {
        collection.deleteOne(eq(ImageDocument::id.name, id))
    }
}
