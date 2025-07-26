package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import java.time.Instant
import java.util.*

interface InternalExchangeShop : ExchangeShop {
    val coroutineScope: CoroutineScope

    fun isUserLoaded(id: UUID): Boolean

    suspend fun loadUser(user: ShopUser)

    suspend fun unloadUser(id: UUID)

    fun getLastUsedTimestamp(user: ShopUser): Instant

    fun setUsed(user: ShopUser)

    suspend fun shutdown()
}
