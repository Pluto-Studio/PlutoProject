package plutoproject.feature.exchangeshop.paper

import plutoproject.feature.exchangeshop.api.paper.ShopUser

interface InternalShopUser : ShopUser {
    suspend fun close()
}
