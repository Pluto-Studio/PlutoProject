package plutoproject.feature.gallery.core.util

import java.util.concurrent.atomic.AtomicBoolean

class CacheEntry<T>(private val value: T) {
    private val lock = Any()
    private var _isDisposed = false
    private var _refCount = 0
    private val handles = LinkedHashSet<Handle<T>>()

    val isDisposed: Boolean
        get() = synchronized(lock) { _isDisposed }

    val refCount: Int
        get() = synchronized(lock) {
            check(!_isDisposed) { "Entry already disposed" }
            _refCount
        }

    fun acquire(): Handle<T> = synchronized(lock) {
        check(!_isDisposed) { "Entry already disposed" }
        val handle = Handle(this)
        handles.add(handle)
        _refCount += 1
        handle
    }

    fun release(handle: Handle<T>) {
        synchronized(lock) {
            if (!handles.remove(handle)) {
                return
            }
            if (_refCount > 0) {
                _refCount -= 1
            }
        }
    }

    fun dispose() {
        val handlesToClose = synchronized(lock) {
            if (_isDisposed) {
                return
            }
            _isDisposed = true
            handles.toList()
        }
        handlesToClose.forEach { it.close() }
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
