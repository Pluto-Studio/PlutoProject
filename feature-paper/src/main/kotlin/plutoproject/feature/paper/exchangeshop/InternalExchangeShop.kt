package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import java.time.Instant
import java.util.*

interface InternalExchangeShop : ExchangeShop {
    val coroutineScope: CoroutineScope

    fun isUserLoaded(id: UUID): Boolean

    suspend fun loadUser(user: InternalShopUser)

    suspend fun unloadUser(id: UUID)

    suspend fun shutdown()
}
