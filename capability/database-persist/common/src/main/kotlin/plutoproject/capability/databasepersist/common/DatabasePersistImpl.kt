package plutoproject.capability.databasepersist.common

import kotlinx.coroutines.*
import plutoproject.capability.databasepersist.api.PersistContainer
import plutoproject.capability.mongo.api.MongoConnection
import java.time.Instant
import java.util.*
import java.util.logging.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class DatabasePersistImpl(
    private val scope: CoroutineScope,
    private val changeStream: DataChangeStream,
    private val repository: ContainerRepository,
    private val autoUnloadCondition: AutoUnloadCondition,
    private val mongoConnection: MongoConnection,
    private val serverIdentifier: String,
    private val logger: Logger,
) : InternalDatabasePersist {
    private val loadedContainers = ConcurrentHashMap<UUID, InternalPersistContainer>()
    private val containerLastUsedTimestamps =
        ConcurrentHashMap<InternalPersistContainer, Instant>()

    @Volatile
    private var isValid = true

    private val autoUnloadDaemonJob: Job = scope.launch {
        while (isValid) {
            delay(AUTO_UNLOAD_INTERVAL_SECONDS.seconds)
            unloadUnusedContainers()
        }
    }

    override fun getContainer(playerId: UUID): PersistContainer {
        check(isValid) { "DatabasePersist instance already closed." }
        return loadedContainers.computeIfAbsent(playerId) {
            PersistContainerImpl(
                playerId = playerId,
                databasePersist = this,
                repository = repository,
                changeStream = changeStream,
                mongoConnection = mongoConnection,
                serverIdentifier = serverIdentifier,
                logger = logger,
            )
        }
    }

    override fun unloadContainer(playerId: UUID): Boolean {
        val container = loadedContainers.remove(playerId) ?: return false
        container.close()
        containerLastUsedTimestamps.remove(container)
        return true
    }

    private fun unloadUnusedContainers() {
        val currentTimestamp = Instant.now()
        val iterator = loadedContainers.entries.iterator()
        while (iterator.hasNext()) {
            val (id, container) = iterator.next()
            val lastUsed = getLastUsedTimestamp(container)
            if (lastUsed.plusSeconds(MAX_UNUSED_SECONDS).isBefore(currentTimestamp)
                && autoUnloadCondition.shouldUnload(id)
            ) {
                container.close()
                iterator.remove()
                containerLastUsedTimestamps.remove(container)
            }
        }
    }

    override fun getLastUsedTimestamp(container: InternalPersistContainer): Instant =
        containerLastUsedTimestamps.getOrPut(container) { Instant.now() }

    override fun setUsed(container: InternalPersistContainer) {
        containerLastUsedTimestamps[container] = Instant.now()
    }

    override fun removeLoadedContainer(container: InternalPersistContainer) {
        loadedContainers.remove(container.playerId)
        containerLastUsedTimestamps.remove(container)
    }

    private fun unloadAllContainers() {
        loadedContainers.values.forEach(InternalPersistContainer::close)
        loadedContainers.clear()
        containerLastUsedTimestamps.clear()
    }

    suspend fun close() {
        if (!isValid) return
        isValid = false
        unloadAllContainers()
        autoUnloadDaemonJob.cancelAndJoin()
        changeStream.close()
    }
}
