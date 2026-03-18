package plutoproject.feature.gallery.core.render.tile.dedupe

internal class IntIndexBucket(initialCapacity: Int = 4) {
    private var values = IntArray(initialCapacity)
    private var size = 0

    fun add(value: Int) {
        if (size == values.size) {
            values = values.copyOf(values.size shl 1)
        }
        values[size] = value
        size++
    }

    fun findFirst(predicate: (Int) -> Boolean): Int? {
        var i = 0
        while (i < size) {
            val value = values[i]
            if (predicate(value)) {
                return value
            }
            i++
        }
        return null
    }
}
