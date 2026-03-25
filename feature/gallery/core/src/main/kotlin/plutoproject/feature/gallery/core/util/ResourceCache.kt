package plutoproject.feature.gallery.core.util

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import java.time.Clock
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

private object ExpiringComparator : Comparator<CacheEntry<*>> {
    override fun compare(o1: CacheEntry<*>, o2: CacheEntry<*>): Int {
        val t1 = requireNotNull(o1.expireAt) { "Cache entry missing expireAt" }
        val t2 = requireNotNull(o2.expireAt) { "Cache entry missing expireAt" }
        return t1.compareTo(t2)
    }
}

abstract class ResourceCache<K, V, I>(
    private val coroutineScope: CoroutineScope,
    private val coroutineContext: CoroutineContext,
    private val clock: Clock,
) {
    private val lock = Any()
    private var isDisposed = false
    private val entriesByKey = mutableMapOf<K, CacheEntry<V>>()
    private val indexesByKey = mutableMapOf<K, I>()
    private val cleanupQueue = PriorityQueue<CacheEntry<V>>(ExpiringComparator)
    private var cleanerJob: Job? = null

    val entries: Map<K, CacheEntry<V>>
        get() = synchronized(lock) { entriesByKey.toMap() }

    protected abstract fun keyOf(value: V): K

    protected abstract fun buildIndex(value: V): I

    protected open fun onEntryAdded(key: K, index: I) = Unit

    protected open fun onEntryRemoved(key: K, index: I) = Unit

    protected open fun onCacheDisposed() = Unit

    private fun ensureCleanerRunning() {
        if (cleanerJob?.isActive == true) {
            return
        }

        cleanerJob = coroutineScope.launch(coroutineContext) {
            runCleaner()
        }.also { job ->
            job.invokeOnCompletion {
                handleCleanerCompletion(it)
            }
        }
    }

    private fun handleCleanerCompletion(cause: Throwable?) {
        if (cause == null || cause is InternalCleanerCancellation || !coroutineScope.isActive) {
            cleanerJob = null
            return
        }

        // TODO: 异常退出警告日志
        ensureCleanerRunning()
    }

    private suspend fun runCleaner() {
        while (true) {
            val pendingEntry = synchronized(lock) { cleanupQueue.poll() } ?: return
            val startTime = clock.instant()
            val expireAt = pendingEntry.expireAt ?: continue

            if (startTime < expireAt) {
                val sleepDuration = Duration.between(startTime, expireAt)
                delay(sleepDuration)
            }

            val key = pendingEntry.withLock { entry ->
                if (entry.expireAt != expireAt) {
                    return@withLock null
                }

                if (entry.refCount >= 1) {
                    pendingEntry.expireAt = null
                    return@withLock null
                }

                entry.acquire().use { handle ->
                    keyOf(handle.value)
                }
            } ?: continue

            synchronized(lock) {
                if (isDisposed || cleanerJob?.isActive != true) {
                    return
                }
                remove(key)
            }
        }
    }

    fun put(key: K, value: V) {
        check(key == keyOf(value)) { "Index and resource key mismatch" }
        synchronized(lock) {
            checkNotDisposed()
            if (entriesByKey.containsKey(key)) {
                return
            }
            val index = buildIndex(value)
            val entry = CacheEntry(value, clock, ::scheduleClean)
            entriesByKey[key] = entry
            indexesByKey[key] = index
            scheduleClean(entry)
            onEntryAdded(key, index)
        }
    }

    private fun scheduleClean(entry: CacheEntry<V>) {
        synchronized(lock) {
            checkNotDisposed()
            checkNotNull(entry.expireAt) { "Cache entry missing expireAt" }
            check(!cleanupQueue.contains(entry)) { "Cleanup already scheduled for this entry" }
            cleanupQueue.add(entry)
            ensureCleanerRunning()
        }
    }

    fun putAll(values: Map<K, V>) {
        if (values.isEmpty()) {
            return
        }
        synchronized(lock) {
            checkNotDisposed()
            values.forEach(::put)
        }
    }

    fun acquire(key: K): CacheEntry.Handle<V>? {
        return synchronized(lock) {
            checkNotDisposed()
            entriesByKey[key]?.acquire()
        }
    }

    fun acquireBatch(keys: Collection<K>): Map<K, CacheEntry.Handle<V>> {
        if (keys.isEmpty()) {
            return emptyMap()
        }
        return synchronized(lock) {
            checkNotDisposed()
            keys.distinct().mapNotNull { key ->
                entriesByKey[key]?.acquire()?.let { key to it }
            }.toMap()
        }
    }

    fun remove(key: K) {
        val entry = synchronized(lock) {
            checkNotDisposed()
            removeEntryWithinLock(key)
        }
        entry?.dispose()
    }

    fun removeAll(keys: Collection<K>) {
        if (keys.isEmpty()) {
            return
        }
        val removedEntries = synchronized(lock) {
            checkNotDisposed()
            keys.mapNotNull { key ->
                removeEntryWithinLock(key)
            }
        }
        removedEntries.forEach(CacheEntry<V>::dispose)
    }

    fun <R> use(key: K, block: (V) -> R): R? {
        val handle = acquire(key) ?: return null
        handle.use { return block(it.value) }
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun dispose() = synchronized(lock) {
        if (isDisposed) {
            return@synchronized
        }

        isDisposed = true
        cleanerJob?.cancel(InternalCleanerCancellation())
        cleanupQueue.clear()
        onCacheDisposed()
        entriesByKey.values.forEach(CacheEntry<V>::dispose)
        indexesByKey.clear()
        entriesByKey.clear()
    }

    protected fun <R> withLock(block: ResourceCache<K, V, I>.() -> R): R = synchronized(lock) {
        checkNotDisposed()
        block()
    }

    // 必须在持有锁的上下文里调用
    private fun removeEntryWithinLock(key: K): CacheEntry<V>? {
        val entry = entriesByKey.remove(key) ?: return null
        val index = indexesByKey.remove(key)
            ?: error("Unexpected: Index metadata not found for existing entry")
        onEntryRemoved(key, index)
        return entry
    }

    private fun checkNotDisposed() {
        check(!isDisposed) { "Resource cache already disposed" }
    }

    private class InternalCleanerCancellation() : CancellationException()
}
