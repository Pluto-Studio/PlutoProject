package plutoproject.feature.gallery.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import plutoproject.feature.gallery.core.RESOURCE_CACHE_TTL_SECONDS
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

abstract class TtlCache<K, V, I>(
    private val coroutineScope: CoroutineScope,
    private val coroutineContext: CoroutineContext,
    private val clock: Clock,
    private val logger: Logger,
) {
    private val lock = Any()
    private val entriesByKey = mutableMapOf<K, CacheEntry<V>>()
    private val indexesByKey = mutableMapOf<K, I>()
    private var cleanerJob: Job? = null
    private var isClosed = false

    protected abstract fun keyOf(value: V): K

    protected abstract fun buildIndex(value: V): I

    protected open fun onEntryAdded(key: K, index: I) = Unit

    protected open fun onEntryRemoved(key: K, index: I) = Unit

    operator fun get(key: K): V? {
        return synchronized(lock) {
            checkNotClosed()
            entriesByKey[key]?.value
        }
    }

    fun getAll(keys: Collection<K>): Map<K, V> {
        if (keys.isEmpty()) {
            return emptyMap()
        }

        return synchronized(lock) {
            checkNotClosed()
            keys.distinct().mapNotNull { key -> entriesByKey[key]?.value?.let { key to it } }.toMap()
        }
    }

    fun put(value: V): V {
        val key = keyOf(value)
        synchronized(lock) {
            checkNotClosed()

            val existingEntry = entriesByKey[key]
            val existingIndex = indexesByKey[key]
            if (existingIndex != null) {
                onEntryRemoved(key, existingIndex)
            }

            val entry = existingEntry ?: CacheEntry(value = value)
            entry.value = value
            if (!entry.isPinned) {
                entry.expiry = entry.expiry ?: nextExpireTime()
                ensureCleanerRunning()
            }

            val index = buildIndex(value)
            entriesByKey[key] = entry
            indexesByKey[key] = index
            onEntryAdded(key, index)
        }
        return value
    }

    fun putAll(values: Collection<V>) {
        values.forEach(::put)
    }

    fun remove(key: K): V? {
        return synchronized(lock) {
            checkNotClosed()
            removeEntry(key)
        }
    }

    fun pin(key: K) {
        synchronized(lock) {
            checkNotClosed()
            val entry = entriesByKey[key] ?: return
            entry.isPinned = true
            entry.expiry = null
        }
    }

    fun unpin(key: K) {
        synchronized(lock) {
            checkNotClosed()
            val entry = entriesByKey[key] ?: return
            entry.isPinned = false
            entry.expiry = nextExpireTime()
            ensureCleanerRunning()
        }
    }

    fun close() {
        val cleaner = synchronized(lock) {
            if (isClosed) {
                return
            }

            isClosed = true
            val keys = entriesByKey.keys.toList()
            keys.forEach(::removeEntry)
            cleanerJob.also { cleanerJob = null }
        }

        cleaner?.cancel(InternalCleanerCancellation())
    }

    private fun nextExpireTime(): Instant {
        return clock.instant().plusSeconds(RESOURCE_CACHE_TTL_SECONDS)
    }

    private fun ensureCleanerRunning() {
        if (cleanerJob?.isActive == true) {
            return
        }

        cleanerJob = coroutineScope.launch(coroutineContext) {
            runCleaner()
        }.also { job ->
            job.invokeOnCompletion { cause ->
                handleCleanerCompletion(cause)
            }
        }
    }

    private fun handleCleanerCompletion(cause: Throwable?) {
        if (cause == null || cause is InternalCleanerCancellation || !coroutineScope.isActive) {
            cleanerJob = null
            return
        }

        logger.log(
            Level.WARNING,
            "An internal error occurred while running cleaner job for ${this::class.simpleName}",
            cause,
        )
        ensureCleanerRunning()
    }

    private suspend fun runCleaner() {
        while (true) {
            val nextCleanup = synchronized(lock) {
                entriesByKey
                    .mapNotNull { (key, entry) -> entry.expiry?.let { key to it } }
                    .minByOrNull { (_, expiry) -> expiry }
            } ?: return

            val (key, expiry) = nextCleanup
            val now = clock.instant()
            if (now < expiry) {
                delay(Duration.between(now, expiry))
            }

            synchronized(lock) {
                if (isClosed || cleanerJob?.isActive != true) {
                    return
                }

                val entry = entriesByKey[key] ?: return@synchronized
                if (entry.expiry != expiry) {
                    return@synchronized
                }
                if (entry.isPinned) {
                    entry.expiry = null
                    return@synchronized
                }

                removeEntry(key)
            }
        }
    }

    private fun removeEntry(key: K): V? {
        val entry = entriesByKey.remove(key) ?: return null
        val index = indexesByKey.remove(key)
            ?: error("Unexpected: index metadata not found for existing cache entry")
        onEntryRemoved(key, index)
        return entry.value
    }

    private fun checkNotClosed() {
        check(!isClosed) { "Ttl cache already closed" }
    }

    private class CacheEntry<V>(
        var value: V,
        var expiry: Instant? = null,
        var isPinned: Boolean = false,
    )

    private class InternalCleanerCancellation : CancellationException()
}
