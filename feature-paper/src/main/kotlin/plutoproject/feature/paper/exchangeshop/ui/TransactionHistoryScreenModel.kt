package plutoproject.feature.paper.exchangeshop.ui

import kotlinx.coroutines.flow.toList
import org.bukkit.OfflinePlayer
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.framework.paper.api.interactive.layout.list.ListMenuModel
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class TransactionHistoryScreenModel(private val shopUser: ShopUser) : ListMenuModel<ShopTransaction>() {
    override suspend fun fetchPageContents(): List<ShopTransaction> {
        val transactionCount = shopUser.countTransactions()
        val skip = page * PAGE_SIZE
        pageCount = ceil(transactionCount.toDouble() / PAGE_SIZE).toInt()
        return shopUser.findTransactions(skip = skip, limit = PAGE_SIZE).toList(mutableListOf())
    }
}
