package plutoproject.feature.warp.paper.screens

import plutoproject.feature.warp.paper.warpManager

import androidx.compose.runtime.mutableStateListOf
import org.bukkit.entity.Player
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpCategory
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.capability.interactive.api.layout.list.FilterListMenuModel

enum class WarpFilter(val filterName: String) {
    ALL("全部"),
    COLLECTED("已收藏"),
    MACHINE("仅看机械类"),
    ARCHITECTURE("仅看建筑类"),
    TOWN("仅看城镇类");

    val category: WarpCategory
        get() = when (this) {
            MACHINE -> WarpCategory.MACHINE
            ARCHITECTURE -> WarpCategory.ARCHITECTURE
            TOWN -> WarpCategory.TOWN
            else -> error("Unexpected")
        }
}

private const val PAGE_SIZE = 28

class WarpListScreenModel(private val player: Player) : FilterListMenuModel<Warp, WarpFilter>(WarpFilter.entries) {
    val collected = mutableStateListOf<Warp>()

    override suspend fun fetchPageContents(): List<Warp> {
        collected.clear()
        return when (filter) {
            WarpFilter.ALL -> {
                pageCount = warpManager.getPageCount(PAGE_SIZE)
                val contents = warpManager.listByPage(PAGE_SIZE, page).toList()
                collected.addAll(contents.filter { warpManager.getCollection(player).contains(it) })
                contents
            }

            WarpFilter.COLLECTED -> {
                pageCount = warpManager.getCollectionPageCount(player, PAGE_SIZE)
                val contents = warpManager.getCollectionByPage(player, PAGE_SIZE, page).toList()
                collected.addAll(contents)
                return contents
            }

            else -> {
                pageCount = warpManager.getPageCount(PAGE_SIZE, filter.category)
                val contents = warpManager.listByPage(PAGE_SIZE, page, filter.category).toList()
                collected.addAll(contents.filter { warpManager.getCollection(player).contains(it) })
                return contents
            }
        }
    }
}
