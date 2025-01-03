package plutoproject.framework.paper.api.interactive.layout.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class FilterListMenuModel<E, F>(filters: List<F>) : ListMenuModel<E>() {
    init {
        require(filters.isNotEmpty()) { "No filter provided" }
    }

    private var filters = filters.distinct()
    var filter by mutableStateOf(filters.first())

    internal fun internalNextFilter() {
        nextFilter()
        page = 0
    }

    internal fun internalPreviousFilter() {
        previousFilter()
        page = 0
    }

    open fun nextFilter() {
        val index = filters.indexOf(filter)
        val nextIndex = index + 1
        val next = if (nextIndex > filters.lastIndex) {
            filters.first()
        } else {
            filters[nextIndex]
        }
        filter = next
    }

    open fun previousFilter() {
        val index = filters.indexOf(filter)
        val previousIndex = index - 1
        val previous = if (previousIndex < 0) {
            filters.last()
        } else {
            filters[previousIndex]
        }
        filter = previous
    }
}
