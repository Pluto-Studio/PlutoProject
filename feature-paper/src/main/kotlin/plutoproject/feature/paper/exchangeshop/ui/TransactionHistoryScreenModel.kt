package plutoproject.feature.paper.exchangeshop.ui

import kotlinx.coroutines.flow.toList
import org.bukkit.OfflinePlayer
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.framework.paper.api.interactive.layout.list.ListMenuModel
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class TransactionHistoryScreenModel(private val player: OfflinePlayer) : ListMenuModel<ShopTransaction>() {
    override suspend fun fetchPageContents(): List<ShopTransaction> {
        val user = ExchangeShop.getUserOrCreate(player)
        val transactionCount = user.countTransactions()
        val skip = page * PAGE_SIZE
        pageCount = ceil(transactionCount.toDouble() / PAGE_SIZE).toInt()
        return user.findTransactions(skip = skip, limit = PAGE_SIZE).toList(mutableListOf())
    }
}
