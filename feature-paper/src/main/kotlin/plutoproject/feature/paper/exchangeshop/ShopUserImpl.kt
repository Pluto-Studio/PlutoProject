package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.flow.Flow
import org.bukkit.OfflinePlayer
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.api.exchangeshop.TransactionFilterDsl
import java.time.Instant
import java.util.*

class ShopUserImpl(
    override val uniqueId: UUID,
    override val player: OfflinePlayer,
    override var ticket: Int,
    private var internalLastTicketRecoveryOn: Instant?
) : ShopUser {
    override val lastTicketRecoveryOn: Instant?
        get() = internalLastTicketRecoveryOn

    override fun withdrawTicket(amount: Int): Result<Int> {
        TODO("Not yet implemented")
    }

    override fun depositTicket(amount: Int): Int {
        TODO("Not yet implemented")
    }

    override suspend fun findTransactions(
        skip: Int?,
        limit: Int?,
        filters: TransactionFilterDsl.() -> Unit
    ): Flow<ShopTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun countTransactions(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun makeTransaction(itemId: String, count: Int): Result<ShopTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun batchTransaction(purchases: Map<String, Int>): Map<String, Result<ShopTransaction>> {
        TODO("Not yet implemented")
    }

    override suspend fun save() {
        TODO("Not yet implemented")
    }
}
