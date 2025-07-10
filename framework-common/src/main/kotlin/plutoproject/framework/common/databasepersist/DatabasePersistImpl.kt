package plutoproject.framework.common.databasepersist

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.common.api.databasepersist.PersistContainer
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.seconds

class DatabasePersistImpl : InternalDatabasePersist, KoinComponent {
    private val collection by inject<MongoCollection<ContainerModel>>()
    private val loadedContainers = mutableConcurrentMapOf<UUID, PersistContainer>()
    private val containerLastUsedTimestamps = mutableConcurrentMapOf<PersistContainer, Instant>()
    private val autoUnloadCondition by inject<AutoUnloadCondition>()
    private var isValid = true

    private val autoUnloadDaemonJob = runAsync {
        while (isValid) {
            delay(AUTO_UNLOAD_INTERVAL_SECONDS.seconds)
            unloadUnusedContainers()
        }
    }

    private fun unloadUnusedContainers() {
        val currentTimestamp = Instant.now()
        val iterator = loadedContainers.entries.iterator()

        // 使用 Iterator 以保证线程安全
        while (iterator.hasNext()) {
            val (id, container) = iterator.next()
            val lastUsed = getLastUsedTimestamp(container)
            if (lastUsed.plusSeconds(MAX_UNUSED_SECONDS).isBefore(currentTimestamp)
                && autoUnloadCondition.shouldUnload(id, container.playerId)
            ) {
                iterator.remove()
                containerLastUsedTimestamps.remove(container)
            }
        }
    }

    override fun getContainer(playerId: UUID): PersistContainer {
        TODO("Not yet implemented")
    }

    override fun getLastUsedTimestamp(container: PersistContainer): Instant {
        return containerLastUsedTimestamps.getValue(container)
    }

    override fun setUsed(container: PersistContainer) {
        containerLastUsedTimestamps[container] = Instant.now()
    }

    override fun close() {
        autoUnloadDaemonJob.cancel()
        isValid = false
    }
}
