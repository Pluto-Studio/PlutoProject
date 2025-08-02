package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.exchangeshop.ExchangeShopConfig
import plutoproject.feature.paper.exchangeshop.SHOW_UNAVAILABLE_ITEMS_DEFAULT
import plutoproject.feature.paper.exchangeshop.SHOW_UNAVAILABLE_ITEMS_PERSIST_KEY
import plutoproject.framework.common.api.databasepersist.adapters.BooleanTypeAdapter
import plutoproject.framework.paper.api.databasepersist.persistContainer
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class ShopCategoryScreenModel(
    private val player: Player,
    private val category: ShopCategory,
) : ScreenModel, KoinComponent {
    private val config by inject<ExchangeShopConfig>()
    private val shopItems = mutableListOf<ShopItem>()
    private var loadAcquires = 0

    var currentPage by mutableStateOf(0)
    var isLoading by mutableStateOf(true)
    var showUnavailableItems by mutableStateOf(false)
    var totalPages by mutableStateOf(0)
    val pageContents = mutableStateListOf<ShopItem>()

    private fun startLoading() {
        loadAcquires++
        isLoading = true
    }

    private fun endLoading() {
        loadAcquires--
        if (loadAcquires == 0) {
            isLoading = false
        }
    }

    fun goPreviousPage() {
        check(currentPage > 0) { "There is no previous page can go" }
        currentPage--
        loadPage()
    }

    fun goNextPage() {
        check(currentPage < totalPages - 1) { "There is no next page can go" }
        currentPage++
        loadPage()
    }

    suspend fun initialize() {
        val container = player.persistContainer
        if (config.capability.availableDays) {
            showUnavailableItems = container.getOrDefault(
                SHOW_UNAVAILABLE_ITEMS_PERSIST_KEY, BooleanTypeAdapter, SHOW_UNAVAILABLE_ITEMS_DEFAULT
            )
        }
        loadPageInitially()
    }

    fun loadPageInitially() {
        startLoading()
        shopItems.clear()
        shopItems.addAll(category.items.filter {
            !config.capability.availableDays || showUnavailableItems || it.isAvailableToday
        })
        totalPages = ceil(shopItems.size.toDouble() / PAGE_SIZE).toInt()
        currentPage = currentPage.coerceIn(0, totalPages - 1)
        loadPage()
        endLoading()
    }

    fun loadPage() {
        startLoading()
        pageContents.clear()
        pageContents.addAll(shopItems.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE))
        endLoading()
    }
}
