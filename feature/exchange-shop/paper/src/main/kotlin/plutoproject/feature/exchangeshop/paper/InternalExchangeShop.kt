package plutoproject.feature.exchangeshop.paper

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.exchangeshop.api.paper.ExchangeShop
import plutoproject.feature.exchangeshop.api.paper.ShopUser
import java.time.Instant
import java.util.*

interface InternalExchangeShop : ExchangeShop {
    val coroutineScope: CoroutineScope

    fun isUserLoaded(id: UUID): Boolean

    suspend fun loadUser(user: InternalShopUser)

    suspend fun unloadUser(id: UUID)

    suspend fun shutdown()
}
