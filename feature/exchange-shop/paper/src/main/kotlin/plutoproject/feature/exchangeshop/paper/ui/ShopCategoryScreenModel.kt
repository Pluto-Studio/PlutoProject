package plutoproject.feature.exchangeshop.paper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import org.bukkit.entity.Player
import plutoproject.kernel.api.koinInject
import plutoproject.feature.exchangeshop.api.paper.ShopCategory
import plutoproject.feature.exchangeshop.api.paper.ShopItem
import plutoproject.feature.exchangeshop.paper.ExchangeShopConfig
import plutoproject.feature.exchangeshop.paper.SHOW_UNAVAILABLE_ITEMS_DEFAULT
import plutoproject.feature.exchangeshop.paper.SHOW_UNAVAILABLE_ITEMS_PERSIST_KEY
import plutoproject.capability.databasepersist.api.adapters.BooleanTypeAdapter
import plutoproject.capability.databasepersist.api.DatabasePersist
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class ShopCategoryScreenModel(
    private val player: Player,
    private val category: ShopCategory,
) : ScreenModel {
    private val config by koinInject<ExchangeShopConfig>()
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
        val container = plutoproject.kernel.api.koinGet<DatabasePersist>().getContainer(player.uniqueId)
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
