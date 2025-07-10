package plutoproject.framework.common.databasepersist

import com.google.common.reflect.TypeToken
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter

data class MemoryEntry<T : Any>(
    val key: String,
    val type: TypeToken<T>,
    val adapter: DataTypeAdapter<T>,
    val value: T,
    val wasChangedSinceLastSave: Boolean
)
