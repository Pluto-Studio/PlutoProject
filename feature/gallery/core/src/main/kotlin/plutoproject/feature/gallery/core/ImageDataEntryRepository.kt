package plutoproject.feature.gallery.core

import java.util.UUID

interface ImageDataEntryRepository {
    suspend fun findByBelongsTo(belongsTo: UUID): ImageDataEntry<*>?

    suspend fun save(entry: ImageDataEntry<*>)

    suspend fun deleteByBelongsTo(belongsTo: UUID)
}
