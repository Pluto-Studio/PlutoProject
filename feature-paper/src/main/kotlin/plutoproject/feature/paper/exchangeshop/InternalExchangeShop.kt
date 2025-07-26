package plutoproject.feature.paper.exchangeshop

import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import java.time.Instant
import java.util.*

interface InternalExchangeShop : ExchangeShop {
    fun isUserLoaded(id: UUID): Boolean

    fun loadUser(user: ShopUser)

    fun unloadUser(id: UUID)

    fun getLastUsedTimestamp(user: ShopUser): Instant

    fun setUsed(user: ShopUser)

    fun shutdown()
}
