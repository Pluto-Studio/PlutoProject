package plutoproject.feature.paper.exchangeshop

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.feature.paper.exchangeshop.ui.ExchangeShop
import plutoproject.feature.paper.exchangeshop.ui.ExchangeShopButtonDescriptor
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.serverName
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger

lateinit var featureLogger: Logger

@Feature(
    id = "exchange_shop",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class ExchangeShopFeature : PaperFeature(), KoinComponent {
    private val exchangeShop by inject<InternalExchangeShop>()
    private val config by inject<ExchangeShopConfig>()
    private val featureModule = module {
        single<ExchangeShopConfig> { loadConfig(saveConfig()) }
        single { ExchangeShopImpl() } binds arrayOf(ExchangeShop::class, InternalExchangeShop::class)
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
        featureLogger = logger
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
        AnnotationParser.parse(TestCommand)
        if (isMenuAvailable) {
            MenuManager.registerButton(ExchangeShopButtonDescriptor) { ExchangeShop() }
        }
        ExchangeShop // 初始化并加载配置定义
    }

    override fun onDisable() = runBlocking {
        exchangeShop.shutdown()
    }
}
