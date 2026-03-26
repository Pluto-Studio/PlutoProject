package plutoproject.feature.gallery.core.util

import plutoproject.feature.gallery.core.RESOURCE_CACHE_TTL_SECONDS
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class CacheEntry<T>(
    private val value: T,
    private val clock: Clock,
    private val scheduleCleanCallback: (CacheEntry<T>) -> Unit
) {
    private val lock = Any()
    private var _isDisposed = false
    private var _refCount = 0
    private var _expireAt: Instant? = nextExpireTime
    private val handles = LinkedHashSet<Handle<T>>()

    val isDisposed: Boolean
        get() = synchronized(lock) { _isDisposed }

    val refCount: Int
        get() = synchronized(lock) { _refCount }

    var expireAt: Instant?
        get() = synchronized(lock) { _expireAt }
        set(value) = synchronized(lock) { _expireAt = value }

    private val nextExpireTime: Instant
        get() = clock.instant().plusSeconds(RESOURCE_CACHE_TTL_SECONDS)

    fun acquire(): Handle<T> = synchronized(lock) {
        check(!_isDisposed) { "Entry already disposed" }
        val handle = Handle(this)
        handles.add(handle)
        _refCount += 1
        handle
    }

    fun release(handle: Handle<T>) {
        val shouldScheduleClean = synchronized(lock) {
            if (!handles.remove(handle)) {
                return@synchronized false
            }
            if (_refCount > 0) {
                _refCount -= 1
            }
            if (_refCount < 1 && _expireAt == null && !_isDisposed) {
                _expireAt = nextExpireTime
                true
            } else {
                false
            }
        }

        if (shouldScheduleClean) {
            scheduleCleanCallback(this)
        }
    }

    fun dispose() {
        val handlesToClose = synchronized(lock) {
            if (_isDisposed) {
                return
            }

            _isDisposed = true
            _refCount = 0
            _expireAt = null
            handles.toList().also { handles.clear() }
        }

        handlesToClose.forEach { it.close() }
    }

    fun <R> withLock(block: (CacheEntry<T>) -> R?): R? {
        synchronized(lock) {
            return block(this)
        }
    }

    private fun getValue(): T = synchronized(lock) {
        check(!_isDisposed) { "Entry already disposed" }
        value
    }

    class Handle<T>(private val entry: CacheEntry<T>) : AutoCloseable {
        private val isClosed = AtomicBoolean(false)

        val value: T
            get() = synchronized(entry.lock) {
                check(!isClosed.get()) { "Handle already closed" }
                return entry.getValue()
            }

        override fun close() = synchronized(entry.lock) {
            if (isClosed.compareAndSet(false, true)) {
                entry.release(this)
            }
        }
    }
}
