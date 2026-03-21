package plutoproject.feature.gallery.core.util

interface ResourceHolder<K, V> {
    val entries: Map<K, ResourceEntry<V>>

    fun put(key: K, value: V)

    fun putAll(values: Map<K, V>)

    fun acquire(key: K): ResourceEntry.Handle<V>?

    fun acquireBatch(keys: Collection<K>): Map<K, ResourceEntry.Handle<V>>

    fun remove(key: K)

    fun removeAll(keys: Collection<K>)

    fun <R> use(key: K, block: (V) -> R): R? {
        val handle = acquire(key) ?: return null
        handle.use { return block(it.value) }
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun dispose()
}
