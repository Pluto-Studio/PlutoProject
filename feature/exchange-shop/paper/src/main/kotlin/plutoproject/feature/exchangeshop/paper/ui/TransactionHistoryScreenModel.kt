package plutoproject.feature.exchangeshop.paper.ui

import kotlinx.coroutines.flow.toList
import org.bukkit.OfflinePlayer
import plutoproject.feature.exchangeshop.api.paper.ExchangeShop
import plutoproject.feature.exchangeshop.api.paper.ShopTransaction
import plutoproject.feature.exchangeshop.api.paper.ShopUser
import plutoproject.capability.interactive.api.layout.list.ListMenuModel
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
