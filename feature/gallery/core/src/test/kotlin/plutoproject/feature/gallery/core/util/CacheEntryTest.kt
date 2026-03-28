package plutoproject.feature.gallery.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CacheEntryTest {
    @Test
    fun `should track ref count while handles are open`() {
        val entry = createEntry()

        val first = entry.acquire()
        val second = entry.acquire()

        assertEquals(2, entry.refCount)

        first.close()
        assertEquals(1, entry.refCount)

        second.close()
        assertEquals(0, entry.refCount)
    }

    @Test
    fun `should reject acquire and value access after dispose`() {
        val entry = createEntry()
        val handle = entry.acquire()

        entry.dispose()

        assertTrue(entry.isDisposed)
        assertThrows(IllegalStateException::class.java) { entry.acquire() }
        assertThrows(IllegalStateException::class.java) { handle.value }
    }

    @Test
    fun `should close existing handles during dispose`() {
        val entry = createEntry()
        val first = entry.acquire()
        val second = entry.acquire()

        entry.dispose()

        assertTrue(entry.isDisposed)
        assertThrows(IllegalStateException::class.java) { first.value }
        assertThrows(IllegalStateException::class.java) { second.value }

        first.close()
        second.close()
        assertFalse(firstValueAccessible(first))
        assertFalse(firstValueAccessible(second))
    }

    private fun firstValueAccessible(handle: CacheEntry.Handle<String>): Boolean =
        runCatching { handle.value }.isSuccess

    private fun createEntry(value: String = "value"): CacheEntry<String> = CacheEntry(
        value = value,
        clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
        scheduleCleanCallback = {}
    )
}
