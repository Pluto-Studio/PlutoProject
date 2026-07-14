package plutoproject.feature.home.paper.screens

import plutoproject.feature.home.paper.homeManager

import org.bukkit.OfflinePlayer
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.capability.interactive.api.layout.list.ListMenuModel
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class HomeListScreenModel(private val viewing: OfflinePlayer) : ListMenuModel<Home>() {
    override suspend fun fetchPageContents(): List<Home> {
        val homes = homeManager.list(viewing).toList()
            .sortedBy {
                when {
                    it.isPreferred -> 0
                    it.isStarred -> 1
                    else -> 2
                }
            }
        pageCount = ceil(homes.size.toDouble() / PAGE_SIZE).toInt()
        return homes.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}
