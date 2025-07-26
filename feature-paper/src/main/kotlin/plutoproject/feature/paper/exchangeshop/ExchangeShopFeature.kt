package plutoproject.feature.paper.exchangeshop

import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.koin.dsl.module
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.serverName
import plutoproject.framework.paper.api.feature.PaperFeature

@Feature(
    id = "exchange_shop",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class ExchangeShopFeature : PaperFeature() {
    private val featureModule = module {
        single<ExchangeShopConfig> { loadConfig(saveConfig()) }
        single<ExchangeShop> { ExchangeShopImpl() }
        single<TransactionRepository> { TransactionRepository(getCollection("transactions")) }
        single<UserRepository> { UserRepository(getCollection("users")) }
    }

    private inline fun <reified T : Any> getCollection(collectionName: String): MongoCollection<T> {
        return MongoConnection.getCollection("exchange_shop_${serverName}_$collectionName")
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
    }
}
