package plutoproject.framework.common.databasepersist

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import plutoproject.framework.common.api.databasepersist.PersistContainer
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import java.util.*

class PersistContainerImpl(
    override val containerId: UUID,
    override val playerId: UUID
) : PersistContainer, KoinComponent {
    private val collection by inject<MongoCollection<ContainerModel>>()
    private val loadedEntries = mutableConcurrentMapOf<String, MemoryEntry<*>>()

    override fun <T> set(key: String, type: DataTypeAdapter<T>, value: T) {
        TODO("Not yet implemented")
    }

    override fun remove(key: String) {
        TODO("Not yet implemented")
    }

    override suspend fun <T> get(key: String, type: DataTypeAdapter<T>): T {
        TODO("Not yet implemented")
    }

    override suspend fun contains(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun save() {
        loadedEntries.forEach { (key, value) ->
            loadedEntries.replace(key, value.copy(wasChangedSinceLastSave = false))
        }
        TODO("Not yet implemented")
    }
}
