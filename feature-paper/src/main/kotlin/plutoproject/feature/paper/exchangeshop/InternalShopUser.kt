package plutoproject.feature.paper.exchangeshop

import plutoproject.feature.paper.api.exchangeshop.ShopUser

interface InternalShopUser : ShopUser {
    suspend fun close()
}
