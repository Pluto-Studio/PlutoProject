package plutoproject.feature.gallery.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

abstract class ResourceCacheTest<K, V> {
    protected abstract fun createCache(): ResourceCache<K, V, *>

    protected abstract fun createValue(seed: Long): V

    protected abstract fun keyOf(value: V): K

    @Test
    fun `should put single value into entries`() {
        val cache = createCache()
        val value = createValue(8000)

        cache.put(keyOf(value), value)

        assertTrue(cache.entries.containsKey(keyOf(value)))
    }

    @Test
    fun `should acquire single loaded value`() {
        val cache = createCache()
        val value = createValue(8001)

        cache.put(keyOf(value), value)

        val handle = cache.acquire(keyOf(value))

        assertEquals(keyOf(value), keyOf(handle!!.value))
        handle.close()
    }

    @Test
    fun `should acquire loaded values and ignore missing ids in batch`() {
        val cache = createCache()
        val first = createValue(8010)
        val second = createValue(8011)

        cache.put(keyOf(first), first)
        cache.put(keyOf(second), second)

        val batch = cache.acquireBatch(listOf(keyOf(first), keyOf(second), keyOf(createValue(8012))))

        assertEquals(setOf(keyOf(first), keyOf(second)), batch.keys)
        batch.values.forEach { it.close() }
    }

    @Test
    fun `should treat duplicate keys in batch as a set`() {
        val cache = createCache()
        val value = createValue(8020)

        cache.put(keyOf(value), value)

        val batch = cache.acquireBatch(listOf(keyOf(value), keyOf(value)))

        assertEquals(setOf(keyOf(value)), batch.keys)
        batch.values.forEach { it.close() }
    }

    @Test
    fun `should dispose removed entry and remove it from entries`() {
        val cache = createCache()
        val value = createValue(8030)

        cache.put(keyOf(value), value)
        val handle = cache.acquire(keyOf(value))

        cache.remove(keyOf(value))

        assertNull(cache.acquire(keyOf(value)))
        assertFalse(cache.entries.containsKey(keyOf(value)))
        assertThrows(IllegalStateException::class.java) { handle!!.value }
    }

    @Test
    fun `should dispose all entries and reject later operations`() {
        val cache = createCache()
        val first = createValue(8040)
        val second = createValue(8041)

        cache.putAll(mapOf(keyOf(first) to first, keyOf(second) to second))
        val handles = cache.acquireBatch(listOf(keyOf(first), keyOf(second))).values.toList()

        cache.dispose()

        assertTrue(cache.entries.isEmpty())
        handles.forEach { handle ->
            assertThrows(IllegalStateException::class.java) { handle.value }
        }
        assertThrows(IllegalStateException::class.java) { cache.put(keyOf(first), first) }
        assertThrows(IllegalStateException::class.java) { cache.acquire(keyOf(first)) }
    }
}
