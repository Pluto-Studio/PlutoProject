package plutoproject.feature.gallery.core.util

abstract class ResourceCache<K, V, I> {
    private val lock = Any()
    private var isDisposed = false
    private val entriesByKey = mutableMapOf<K, CacheEntry<V>>()
    private val indexesByKey = mutableMapOf<K, I>()

    val entries: Map<K, CacheEntry<V>>
        get() = synchronized(lock) { entriesByKey.toMap() }

    protected abstract fun keyOf(value: V): K

    protected abstract fun buildIndex(value: V): I

    protected open fun onEntryAdded(key: K, index: I) = Unit

    protected open fun onEntryRemoved(key: K, index: I) = Unit

    protected open fun onCacheDisposed() = Unit

    fun put(key: K, value: V) {
        check(key == keyOf(value)) { "Index and resource key mismatch" }
        synchronized(lock) {
            checkNotDisposed()
            if (entriesByKey.containsKey(key)) {
                return
            }
            val index = buildIndex(value)
            entriesByKey[key] = CacheEntry(value)
            indexesByKey[key] = index
            onEntryAdded(key, index)
        }
    }

    fun putAll(values: Map<K, V>) {
        if (values.isEmpty()) {
            return
        }
        synchronized(lock) {
            checkNotDisposed()
            values.forEach { (key, value) ->
                if (entriesByKey.containsKey(key)) {
                    return@forEach
                }
                check(key == keyOf(value)) { "Index and resource key mismatch" }
                val index = buildIndex(value)
                entriesByKey[key] = CacheEntry(value)
                indexesByKey[key] = index
                onEntryAdded(key, index)
            }
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

    fun dispose() {
        val entriesToDispose = synchronized(lock) {
            if (isDisposed) {
                return@synchronized null
            }
            isDisposed = true
            onCacheDisposed()
            indexesByKey.clear()
            entriesByKey.values.toList().also { entriesByKey.clear() }
        }
        entriesToDispose?.forEach(CacheEntry<V>::dispose)
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
        check(!isDisposed) { "Resources already disposed" }
    }
}
