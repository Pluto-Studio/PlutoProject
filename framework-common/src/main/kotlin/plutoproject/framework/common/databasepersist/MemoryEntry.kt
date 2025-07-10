package plutoproject.framework.common.databasepersist

import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

data class MemoryEntry<T : Any>(
    val key: String,
    val type: Class<*>,
    val adapter: DataTypeAdapter<T>,
    val value: T,
    val wasChangedSinceLastSave: Boolean
)
