package ink.pmc.framework.interactive.inventory.layout.list_menu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class FilterListMenuModel<E, F>(pageSize: Int, filters: List<F>) : ListMenuModel<E>(pageSize) {
    init {
        require(filters.isNotEmpty()) { "No filter provided" }
    }

    private var filters = filters.distinct()
    var filter by mutableStateOf(filters.first())

    fun nextFilter() {
        val index = filters.indexOf(filter)
        val nextIndex = index + 1
        val next = if (nextIndex > filters.lastIndex) {
            filters.first()
        } else {
            filters[nextIndex]
        }
        filter = next
    }

    fun previousFilter() {
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