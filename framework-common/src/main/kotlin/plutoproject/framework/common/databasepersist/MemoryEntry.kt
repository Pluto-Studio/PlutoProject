package plutoproject.framework.common.databasepersist

import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

data class MemoryEntry<T>(
    val key: String,
    val type: DataTypeAdapter<T>,
    val value: T,
    val wasChangedSinceLastSave: Boolean
)
