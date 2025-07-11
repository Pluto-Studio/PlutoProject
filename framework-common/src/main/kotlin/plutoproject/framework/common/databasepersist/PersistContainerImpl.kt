package plutoproject.framework.common.databasepersist

import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.client.model.changestream.OperationType.*
import org.bson.BsonDocument
import org.bson.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import plutoproject.framework.common.api.databasepersist.PersistContainer
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.common.util.data.containsNested
import plutoproject.framework.common.util.data.getNestedValue
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.common.util.data.setNestedValue
import plutoproject.framework.common.util.serverName
import java.time.Instant
import java.util.*

class PersistContainerImpl(override val playerId: UUID) : PersistContainer, KoinComponent {
    private val databasePersist by inject<InternalDatabasePersist>()
    private val repository by inject<ContainerRepository>()
    private val changeStream by inject<DataChangeStream>()

    private val loadedEntries = mutableConcurrentMapOf<String, MemoryEntry<*>>()
    private val removedEntries = mutableConcurrentSetOf<String>()
    private var isValid = true

    private val String.isValidKey: Boolean
        get() = !startsWith(".") && !endsWith(".")

    init {
        changeStream.subscribe(playerId) { event ->
            when (event.operationType) {
                INSERT -> onFullDocumentChange(event.fullDocument!!)
                UPDATE -> onFullDocumentChange(event.fullDocument!!)
                REPLACE -> onPartialDocumentChange(
                    event.updateDescription!!.updatedFields!!,
                    event.updateDescription!!.removedFields!!
                )

                else -> error("Unexpected")
            }
        }
    }

    private fun updateMemoryEntries(changed: BsonDocument) {
        loadedEntries
            .filter { (key, _) -> changed.containsNested(key) }
            .forEach { (key, entry) ->
                val new = entry.copy(value = changed.getNestedValue(key)!!, wasChangedSinceLastSave = false)
                loadedEntries.replace(key, new)
            }
        removedEntries.removeIf { key -> changed.containsKey(key) }
    }

    private fun onFullDocumentChange(document: Document) {
        updateMemoryEntries(document.toBsonDocument())
    }

    private fun onPartialDocumentChange(updated: BsonDocument, removed: List<String>) {
        updateMemoryEntries(updated)
        removed.forEach { key -> loadedEntries.remove(key) }
    }

    override fun <T : Any> set(key: String, adapter: DataTypeAdapter<T>, value: T) {
        check(isValid) { "PersistContainer instance already unloaded." }
        require(key.isValidKey) { "$key is not a valid key." }
        removedEntries.remove(key)
        databasePersist.setUsed(this)

        if (loadedEntries.contains(key)) {
            val entry = loadedEntries.getValue(key)
            require(adapter.type.isInstance(value)) { "Value isn't an instance of ${adapter.type.qualifiedName}." }
            require(adapter.type == entry.adapter.type) {
                "Type mismatch for entry with key $key: attempting to set type ${adapter.type.qualifiedName}, but found ${entry.adapter.type.qualifiedName} in memory."
            }
            loadedEntries.replace(key, entry.copy(value = adapter.toBson(value), wasChangedSinceLastSave = true))
            return
        }

        loadedEntries[key] = MemoryEntry(key, adapter.toBson(value), adapter, true)
    }

    override fun remove(key: String) {
        loadedEntries.remove(key)
        removedEntries += key
    }

    override suspend fun <T : Any> get(key: String, adapter: DataTypeAdapter<T>): T? {
        check(isValid) { "PersistContainer instance already unloaded." }
        require(key.isValidKey) { "$key is not a valid key." }
        databasePersist.setUsed(this)

        if (removedEntries.contains(key)) {
            return null
        }

        if (loadedEntries.contains(key)) {
            val entry = loadedEntries.getValue(key)
            require(adapter.type == entry.adapter.type) {
                "Type mismatch for entry with key $key: attempting to get type ${adapter.type.qualifiedName}, but found ${entry.adapter.type.qualifiedName} in memory."
            }
            return adapter.fromBson(entry.value)
        }

        val projection = Projections.include("data.$key")
        val document = repository.findByPlayerId(playerId, projection)
            ?.toBsonDocument(BsonDocument::class.java, Provider.mongoClient.codecRegistry) ?: return null
        val value = document.getNestedValue("data.$key") ?: return null
        val entry = MemoryEntry(key, value, adapter, false)

        loadedEntries[key] = entry
        return adapter.fromBson(value)
    }

    override suspend fun contains(key: String): Boolean {
        check(isValid) { "PersistContainer instance already unloaded." }
        require(key.isValidKey) { "$key is not a valid key." }
        databasePersist.setUsed(this)

        if (removedEntries.contains(key)) {
            return false
        }
        if (loadedEntries.contains(key)) {
            return true
        }

        val projection = Projections.include("data.$key")
        val document = repository.findByPlayerId(playerId, projection)
            ?.toBsonDocument(BsonDocument::class.java, Provider.mongoClient.codecRegistry) ?: return false

        document.getNestedValue("data.$key") ?: return false
        return true
    }

    override suspend fun save() {
        check(isValid) { "PersistContainer instance already unloaded." }
        databasePersist.setUsed(this)
        val projection = Projections.include("playerId")
        val document = repository.findByPlayerId(playerId, projection)

        if (document == null) {
            val timestamp = Instant.now().toEpochMilli()
            val data = BsonDocument()
            loadedEntries.forEach { (key, entry) ->
                data.setNestedValue(key, entry.value)
                loadedEntries.replace(key, entry.copy(wasChangedSinceLastSave = false))
            }
            val model = ContainerModel(
                playerId = playerId,
                createdAt = timestamp,
                updateInfo = UpdateInfo(playerId, serverName, timestamp),
                data = data
            )
            removedEntries.clear()
            repository.save(model)
            return
        }

        val updates = mutableListOf(
            Updates.set(
                ContainerModel::updateInfo.name,
                UpdateInfo(playerId, serverName, Instant.now().toEpochMilli())
            )
        )

        if (removedEntries.isNotEmpty()) {
            updates.addAll(removedEntries.map { key -> Updates.unset("data.$key") })
            removedEntries.clear()
        }

        loadedEntries
            .filter { (_, entry) -> entry.wasChangedSinceLastSave }
            .forEach { (key, entry) ->
                updates.add(Updates.set("data.$key", entry.value))
                loadedEntries.replace(key, entry.copy(wasChangedSinceLastSave = false))
            }

        repository.updateDocument(playerId, Updates.combine(updates))
    }

    override fun close() {
        isValid = false
        changeStream.unsubscribe(playerId)
    }
}
