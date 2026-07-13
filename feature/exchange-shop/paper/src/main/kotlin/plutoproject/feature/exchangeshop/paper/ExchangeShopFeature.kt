package plutoproject.feature.exchangeshop.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.incendo.cloud.parser.ParserDescriptor
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.capability.serveridentifier.api.ServerIdentifier
import plutoproject.feature.exchangeshop.api.paper.ExchangeShop
import plutoproject.feature.exchangeshop.api.paper.ShopCategory
import plutoproject.feature.exchangeshop.api.paper.ShopUser
import plutoproject.feature.exchangeshop.paper.commands.*
import plutoproject.feature.exchangeshop.paper.repositories.TransactionRepository
import plutoproject.feature.exchangeshop.paper.repositories.UserRepository
import plutoproject.feature.exchangeshop.paper.ui.ExchangeShop as ExchangeShopScreen
import plutoproject.feature.exchangeshop.paper.ui.ExchangeShopButtonDescriptor
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.logging.Logger

lateinit var featureLogger: Logger

@Feature(
    id = "exchange_shop",
    platform = Platform.PAPER,
    optionalFeatures = ["menu"],
    requiredFeatures = ["daily"],
    requiredCapabilities = ["mongo", "server_identifier", "database_persist", "interactive", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
class ExchangeShopFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(ExchangeShopFeature::class.java.classLoader)
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<ExchangeShopConfig>()
        val serverId = context.services.getService<ServerIdentifier>().identifierOrThrow()
        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<DatabasePersist>()
        context.importServiceToKoin<GuiManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single { ExchangeShopImpl() } binds arrayOf(ExchangeShop::class, InternalExchangeShop::class)
            single { TransactionRepository(collection(context, serverId, "transactions")) }
            single { UserRepository(collection(context, serverId, "users")) }
        })
        context.services.exportServiceFromKoin<ExchangeShop>()
    }

    private inline fun <reified T : Any> collection(
        context: ModuleContext,
        serverId: String,
        name: String,
    ): MongoCollection<T> = context.koinGet<MongoConnection>()
        .getCollection("exchange_shop_${serverId}_$name")

    override suspend fun onEnable(context: ModuleContext) {
        val paper = context as PaperModuleContext
        featureLogger = context.logger
        paper.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, paper.plugin)
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        parser.manager().parserRegistry().apply {
            registerSuggestionProvider("shop-category", ShopCategoryParser)
            registerNamedParser("shop-category", ParserDescriptor.of(ShopCategoryParser, ShopCategory::class.java))
            registerSuggestionProvider("shop-user", ShopUserParser)
            registerNamedParser("shop-user", ParserDescriptor.of(ShopUserParser, ShopUser::class.java))
        }
        commands = CloudCommandRegistration.register(
            parser,
            ExchangeShopCommand,
            ShopCategoryNotFoundExceptionHandler,
            ShopUserNotFoundExceptionHandler,
        )
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(ExchangeShopButtonDescriptor) { ExchangeShopScreen() }
        context.koinGet<ExchangeShop>()
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(PlayerListener)
        context.koinGet<InternalExchangeShop>().shutdown()
    }
}
