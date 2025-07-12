package plutoproject.framework.common.databasepersist

import org.bson.BsonValue
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

data class MemoryEntry<T : Any>(
    val key: String,
    val value: BsonValue,
    val adapter: DataTypeAdapter<T>,
    val wasChangedSinceLastSave: Boolean,
)
