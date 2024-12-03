package ink.pmc.framework.interactive.inventory.layout.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel

abstract class ListMenuModel<E>() : ScreenModel {
    var isLoading by mutableStateOf(true)
    var pageCount by mutableStateOf(0)
    var page by mutableStateOf(0)
    val contents = mutableStateListOf<E>()

    suspend fun loadPageContents() {
        isLoading = true
        pageCount = 0
        contents.clear()
        contents.addAll(fetchPageContents())
        val range = 0 until pageCount
        check(page in range || range.isEmpty()) { "Page $page must be in range: $range" }
        isLoading = false
    }

    abstract suspend fun fetchPageContents(): List<E>

    open fun previousPage(): Boolean {
        return if (page > 0) {
            page--
            true
        } else false
    }

    open fun nextPage(): Boolean {
        return if (page < pageCount - 1) {
            page++
            true
        } else false
    }
}