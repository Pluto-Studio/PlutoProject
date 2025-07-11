package plutoproject.framework.common.databasepersist

import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.OperationType
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.DataTypeAdapter
import plutoproject.framework.common.api.databasepersist.PersistContainer
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.util.data.collection.mutableConcurrentSetOf
import plutoproject.framework.common.util.data.flatten
import plutoproject.framework.common.util.data.getNested
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.common.util.data.setNested
import plutoproject.framework.common.util.data.toBsonDocument
import plutoproject.framework.common.util.logger
import plutoproject.framework.common.util.serverName
import java.time.Instant
import java.util.*
import java.util.logging.Level

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
            runCatching {
                update(event)
            }.onFailure {
                logger.log(Level.SEVERE, "Exception caught while trying to update container $playerId", it)
            }
        }
    }

    private fun update(event: ChangeStreamDocument<Document>) {
        when (event.operationType) {
            in FULL_DOCUMENT_OPERATION_TYPES -> {
                val fullDocument = event.fullDocument?.getValue("data") as? BsonDocument ?: return
                onFullDocumentChange(fullDocument.flatten())
            }

            OperationType.UPDATE -> {
                val updateDescription = event.updateDescription ?: return
                val updatedFields = updateDescription.updatedFields?.entries
                    ?.filter { it.key.startsWith("data.") }
                    ?.associate { it.key.removePrefix("data.") to it.value }?.toBsonDocument() ?: return
                val removedFields = updateDescription.removedFields?.map { it.removePrefix("data.") }
                onPartialDocumentChange(updatedFields, removedFields ?: emptyList())
            }

            else -> error("Unexpected")
        }
    }

    private fun updateMemoryEntries(data: BsonDocument) {
        data.forEach { (key, value) ->
            when {
                // 完全匹配
                loadedEntries.containsKey(key) -> updateIdenticalPathInMemory(key, value)
                // 内存比数据库更深
                loadedEntries.any { it.key.startsWith("$key.") } -> updateDeeperPathInMemory(key, value)
                // 内存比数据库更浅
                loadedEntries.any { key.startsWith("${it.key}.") } -> updateShallowerPathInMemory(key, value)
            }
        }
    }

    private fun updateIdenticalPathInMemory(key: String, value: BsonValue) {
        check(isValid) { "PersistContainer instance already closed." }
        val entry = loadedEntries.getValue(key)
        val newEntry = entry.copy(value = value, wasChangedSinceLastSave = false)
        loadedEntries.replace(key, newEntry)
    }

    private fun updateDeeperPathInMemory(key: String, value: BsonValue) {
        check(isValid) { "PersistContainer instance already closed." }
        val outerDocument = value as BsonDocument
        loadedEntries.filterKeys { it.startsWith("$key.") }
            .forEach { (deeperKey, entry) ->
                val relativePath = deeperKey.removePrefix("$key.")
                val changedValue = outerDocument.getNested(relativePath) ?: return@forEach
                val updatedEntry = entry.copy(value = changedValue, wasChangedSinceLastSave = false)
                loadedEntries.replace(deeperKey, updatedEntry)
            }
    }

    private fun updateShallowerPathInMemory(key: String, value: BsonValue) {
        check(isValid) { "PersistContainer instance already closed." }
        loadedEntries.filterKeys { key.startsWith("$it.") }
            .forEach { (shallowerKey, entry) ->
                val relativePath = key.removePrefix("$shallowerKey.")
                val outerDocument = entry.value as? BsonDocument ?: return@forEach
                val updatedEntry = entry.copy(
                    value = outerDocument.clone().apply { setNested(relativePath, value) },
                    wasChangedSinceLastSave = false
                )
                loadedEntries.replace(shallowerKey, updatedEntry)
            }
    }

    private fun removeIdenticalPathInMemory(key: String) {
        check(isValid) { "PersistContainer instance already closed." }
        loadedEntries.remove(key)
    }

    private fun removeDeeperPathInMemory(key: String) {
        check(isValid) { "PersistContainer instance already closed." }
        loadedEntries.keys.removeIf { it.startsWith("$key.") }
    }

    private fun removeShallowerPathInMemory(key: String) {
        check(isValid) { "PersistContainer instance already closed." }
        loadedEntries.filterKeys { key.startsWith("$it.") }
            .forEach { (shallowerKey, entry) ->
                val relativePath = key.removePrefix("$shallowerKey.")
                val outerDocument = entry.value as? BsonDocument ?: return@forEach
                val updatedEntry = entry.copy(
                    value = outerDocument.clone().apply { remove(relativePath) },
                    wasChangedSinceLastSave = false
                )
                loadedEntries.replace(shallowerKey, updatedEntry)
            }
    }

    private fun onFullDocumentChange(document: BsonDocument) {
        updateMemoryEntries(document)
    }

    private fun removeMemoryEntries(removed: List<String>) {
        removed.forEach { key ->
            when {
                loadedEntries.containsKey(key) -> removeIdenticalPathInMemory(key)
                loadedEntries.any { it.key.startsWith("$key.") } -> removeDeeperPathInMemory(key)
                loadedEntries.any { key.startsWith("${it.key}.") } -> removeShallowerPathInMemory(key)
            }
        }
    }

    private fun onPartialDocumentChange(updated: BsonDocument, removed: List<String>) {
        updateMemoryEntries(updated)
        removeMemoryEntries(removed)
    }

    override fun <T : Any> set(key: String, adapter: DataTypeAdapter<T>, value: T) {
        check(isValid) { "PersistContainer instance already closed." }
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
        removeMemoryEntries(listOf(key))
        removedEntries += key
    }

    override suspend fun <T : Any> get(key: String, adapter: DataTypeAdapter<T>): T? {
        check(isValid) { "PersistContainer instance already closed." }
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
        val value = document.getNested("data.$key") ?: return null
        val entry = MemoryEntry(key, value, adapter, false)

        loadedEntries[key] = entry
        return adapter.fromBson(value)
    }

    override suspend fun <T : Any> getOrDefault(key: String, adapter: DataTypeAdapter<T>, default: T): T {
        check(isValid) { "PersistContainer instance already closed." }
        return get(key, adapter) ?: default
    }

    override suspend fun <T : Any> getOrElse(key: String, adapter: DataTypeAdapter<T>, defaultValue: () -> T): T {
        check(isValid) { "PersistContainer instance already closed." }
        return get(key, adapter) ?: defaultValue()
    }

    override suspend fun <T : Any> getOrSet(key: String, adapter: DataTypeAdapter<T>, defaultValue: () -> T): T {
        check(isValid) { "PersistContainer instance already closed." }
        val value = get(key, adapter)
        return if (value == null) {
            val result = defaultValue()
            set(key, adapter, result)
            result
        } else {
            value
        }
    }

    override suspend fun contains(key: String): Boolean {
        check(isValid) { "PersistContainer instance already closed." }
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

        document.getNested("data.$key") ?: return false
        return true
    }

    override suspend fun save() {
        check(isValid) { "PersistContainer instance already closed." }
        databasePersist.setUsed(this)
        val projection = Projections.include("playerId")
        val document = repository.findByPlayerId(playerId, projection)

        if (document == null) {
            val timestamp = Instant.now().toEpochMilli()
            val data = BsonDocument()
            loadedEntries.forEach { (key, entry) ->
                data.setNested(key, entry.value)
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
        changeStream.unsubscribe(playerId)
        isValid = false
    }
}
