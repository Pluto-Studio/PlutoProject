package ink.pmc.essentials.screens.warp

import androidx.compose.runtime.mutableStateListOf
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpCategory
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.interactive.layout.list.FilterListMenuModel
import org.bukkit.entity.Player

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
                pageCount = WarpManager.getPageCount(PAGE_SIZE)
                val contents = WarpManager.listByPage(PAGE_SIZE, page).toList()
                collected.addAll(contents.filter { WarpManager.getCollection(player).contains(it) })
                contents
            }

            WarpFilter.COLLECTED -> {
                pageCount = WarpManager.getCollectionPageCount(player, PAGE_SIZE)
                val contents = WarpManager.getCollectionByPage(player, PAGE_SIZE, page).toList()
                collected.addAll(contents)
                return contents
            }

            else -> {
                pageCount = WarpManager.getPageCount(PAGE_SIZE, filter.category)
                val contents = WarpManager.listByPage(PAGE_SIZE, page, filter.category).toList()
                collected.addAll(contents.filter { WarpManager.getCollection(player).contains(it) })
                return contents
            }
        }
    }
}