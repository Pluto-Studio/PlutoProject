package plutoproject.feature.paper.exchangeshop

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.parser.ParserDescriptor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.exchangeshop.commands.*
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.feature.paper.exchangeshop.ui.ExchangeShop
import plutoproject.feature.paper.exchangeshop.ui.ExchangeShopButtonDescriptor
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.serverName
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.command.CommandManager
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger

lateinit var featureLogger: Logger

@Feature(
    id = "exchange_shop",
    platform = Platform.PAPER,
    dependencies = [Dependency(id = "menu", required = false)]
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
        initializeCommands()
        if (isMenuAvailable) {
            MenuManager.registerButton(ExchangeShopButtonDescriptor) { ExchangeShop() }
        }
        ExchangeShop // 初始化并加载配置定义
    }

    @Suppress("UnstableApiUsage")
    private fun initializeCommands() {
        // 2026.2.1 - 更新访客模式 - 最小权限，不再默认给予任何权限节点
        /*
        val permissions = listOf(
            Permission(COMMAND_EXCHANGE_SHOP_TRANSACTIONS_PERMISSION, PermissionDefault.OP),
            Permission(COMMAND_EXCHANGE_SHOP_TICKET_PERMISSION, PermissionDefault.OP),
            Permission(COMMAND_EXCHANGE_SHOP_TICKET_SET_PERMISSION, PermissionDefault.OP),
            Permission(COMMAND_EXCHANGE_SHOP_TICKET_WITHDRAW_PERMISSION, PermissionDefault.OP),
            Permission(COMMAND_EXCHANGE_SHOP_TICKET_DEPOSIT_PERMISSION, PermissionDefault.OP),
            Permission(COMMAND_EXCHANGE_SHOP_STATS_PERMISSION, PermissionDefault.OP),
        )
        */
        // server.pluginManager.addPermissions(permissions)
        CommandManager.parserRegistry().apply {
            registerSuggestionProvider("shop-category", ShopCategoryParser)
            registerNamedParser("shop-category", ParserDescriptor.of(ShopCategoryParser, ShopCategory::class.java))
            registerSuggestionProvider("shop-user", ShopUserParser)
            registerNamedParser("shop-user", ParserDescriptor.of(ShopUserParser, ShopUser::class.java))
        }
        AnnotationParser.parse(
            ExchangeShopCommand,
            ShopCategoryNotFoundExceptionHandler,
            ShopUserNotFoundExceptionHandler,
        )
    }

    override fun onDisable() = runBlocking {
        exchangeShop.shutdown()
    }
}
