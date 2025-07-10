package plutoproject.framework.common.databasepersist

import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import org.bson.BsonDocument
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import plutoproject.framework.common.api.databasepersist.PersistContainer
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.common.util.data.getNestedValue
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.common.util.data.setNestedValue
import java.time.Instant
import java.util.*

@Suppress("UNCHECKED_CAST")
class PersistContainerImpl(override val playerId: UUID) : PersistContainer, KoinComponent {
    private val databasePersist by inject<InternalDatabasePersist>()
    private val repository by inject<ContainerRepository>()
    private val loadedEntries = mutableConcurrentMapOf<String, MemoryEntry<*>>()
    private val removedEntries = mutableConcurrentSetOf<String>()

    private val String.isValidKey: Boolean
        get() = !startsWith(".") && !endsWith(".")

    override fun <T : Any> set(key: String, adapter: DataTypeAdapter<T>, value: T) {
        require(key.isValidKey) { "$key is not a valid key." }
        removedEntries.remove(key)
        databasePersist.setUsed(this)

        if (loadedEntries.contains(key)) {
            val entry = loadedEntries.getValue(key)
            require(adapter.type.isSubtypeOf(entry.type)) {
                "Type mismatch for entry with key $key: attempting to set type ${adapter.type}, but found ${entry.type} in memory."
            }
            loadedEntries.replace(key, (entry as MemoryEntry<T>).copy(value = value, wasChangedSinceLastSave = true))
            return
        }

        loadedEntries[key] = MemoryEntry(key, adapter.type, adapter, value, true)
    }

    override fun remove(key: String) {
        loadedEntries.remove(key)
        removedEntries += key
    }

    override suspend fun <T : Any> get(key: String, adapter: DataTypeAdapter<T>): T? {
        require(key.isValidKey) { "$key is not a valid key." }
        databasePersist.setUsed(this)

        if (removedEntries.contains(key)) {
            return null
        }

        if (loadedEntries.contains(key)) {
            val entry = loadedEntries.getValue(key)
            require(adapter.type.isSubtypeOf(entry.type)) {
                "Type mismatch for entry with key $key: attempting to get type ${adapter.type}, but found ${entry.type} in memory."
            }
            return entry.value as T
        }

        val projection = Projections.include("data.$key")
        val document = repository.findByPlayerId(playerId, projection)?.toBsonDocument() ?: return null

        val data = document.getNestedValue("data.$key") ?: return null
        val value = adapter.fromBson(data)
        val entry = MemoryEntry(key, adapter.type, adapter, value, false)

        loadedEntries[key] = entry
        return value
    }

    override suspend fun contains(key: String): Boolean {
        require(key.isValidKey) { "$key is not a valid key." }
        databasePersist.setUsed(this)

        if (removedEntries.contains(key)) {
            return false
        }
        if (loadedEntries.contains(key)) {
            return true
        }

        val projection = Projections.include("data.$key")
        val document = repository.findByPlayerId(playerId, projection)?.toBsonDocument() ?: return false

        document.getNestedValue("data.$key") ?: return false
        return true
    }

    override suspend fun save() {
        databasePersist.setUsed(this)
        val projection = Projections.include("playerId")
        val document = repository.findByPlayerId(playerId, projection)

        if (document == null) {
            val timestamp = Instant.now().toEpochMilli()
            val data = BsonDocument()
            loadedEntries.forEach { (key, entry) ->
                val adapter = entry.adapter as DataTypeAdapter<Any>
                val value = entry.value
                data.setNestedValue(key, adapter.toBson(value))
                loadedEntries.replace(key, entry.copy(wasChangedSinceLastSave = false))
            }
            val model = ContainerModel(
                playerId = playerId,
                createdAt = timestamp,
                updatedAt = timestamp,
                data = data
            )
            removedEntries.clear()
            repository.save(model)
            return
        }

        val updates = mutableListOf(Updates.set("updatedAt", Instant.now().toEpochMilli()))

        if (removedEntries.isNotEmpty()) {
            updates.addAll(removedEntries.map { key -> Updates.unset("data.$key") })
            removedEntries.clear()
        }

        loadedEntries
            .filter { (_, entry) -> entry.wasChangedSinceLastSave }
            .forEach { (key, entry) ->
                val adapter = entry.adapter as DataTypeAdapter<Any>
                val value = entry.value
                updates.add(Updates.set("data.$key", adapter.toBson(value)))
                loadedEntries.replace(key, entry.copy(wasChangedSinceLastSave = false))
            }

        repository.updateDocument(playerId, Updates.combine(updates))
    }
}
