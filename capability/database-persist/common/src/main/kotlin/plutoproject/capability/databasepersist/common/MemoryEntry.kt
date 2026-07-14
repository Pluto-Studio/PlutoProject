package plutoproject.capability.databasepersist.common

import org.bson.BsonValue
import plutoproject.capability.databasepersist.api.DataTypeAdapter

data class MemoryEntry<T : Any>(
    val key: String,
    val value: BsonValue,
    val adapter: DataTypeAdapter<T>,
    val wasChangedSinceLastSave: Boolean,
)
