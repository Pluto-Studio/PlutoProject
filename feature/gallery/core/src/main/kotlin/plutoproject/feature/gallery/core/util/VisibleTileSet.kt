package plutoproject.feature.gallery.core.util

import java.util.*

class VisibleTileSet(capacity: Int) {
    private val marks = IntArray(capacity)
    private val values = IntArray(capacity)
    private var epoch = 1

    var size = 0
        private set

    init {
        require(capacity > 0) { "capacity must be > 0" }
    }

    fun clear() {
        size = 0
        epoch++
        if (epoch == Int.MAX_VALUE) {
            Arrays.fill(marks, 0)
            epoch = 1
        }
    }

    fun add(tile: Int) {
        require(tile in values.indices) { "Tile index out of range: tile=$tile, capacity=${values.size}" }
        if (marks[tile] != epoch) {
            marks[tile] = epoch
            values[size++] = tile
        }
    }

    fun forEach(block: (Int) -> Unit) {
        for (index in 0 until size) {
            block(values[index])
        }
    }
}
