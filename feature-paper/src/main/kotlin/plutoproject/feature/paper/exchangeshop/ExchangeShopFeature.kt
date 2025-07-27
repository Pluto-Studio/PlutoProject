package plutoproject.feature.paper.exchangeshop

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.mongodb.kotlin.client.coroutine.MongoCollection
import ink.pmc.advkt.component.text
import ink.pmc.advkt.showTitle
import ink.pmc.advkt.title.*
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.exchangeshop.repositories.TransactionRepository
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.serverName
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.time.Duration
import java.time.Instant
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

lateinit var featureLogger: Logger

@Feature(
    id = "exchange_shop",
    platform = Platform.PAPER
)
@Suppress("UNUSED")
class ExchangeShopFeature : PaperFeature(), KoinComponent {
    private val exchangeShop by inject<InternalExchangeShop>()
    private val config by inject<ExchangeShopConfig>()
    private var ticketRecoveryTitleJob: Job? = null
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
        ExchangeShop // 初始化并加载配置定义
        ticketRecoveryTitleJob = exchangeShop.coroutineScope.launch {
            while (isActive) {
                server.onlinePlayers.forEach { player ->
                    if (!player.isConnected) return@forEach
                    val user = exchangeShop.getUser(player) ?: return@forEach
                    val seconds = Duration
                        .between(Instant.now(), user.scheduledTicketRecoveryOn ?: return@forEach)
                        .toSeconds() + 1
                    player.showTitle {
                        mainTitle(Component.empty())
                        subTitle {
                            text("将在 ") with mochaText
                            text("$seconds 秒 ") with mochaLavender
                            text("后恢复 ") with mochaText
                            text("(") with mochaText
                            text("${user.ticket}/${config.ticket.recoveryCap}") with mochaLavender
                            text(")") with mochaText
                        }
                        times {
                            fadeIn(0.seconds)
                            stay(2.seconds)
                            fadeOut(0.seconds)
                        }
                    }
                }
                delay(100.milliseconds)
            }
        }
    }

    override fun onDisable() = runBlocking {
        ticketRecoveryTitleJob?.cancelAndJoin()
        exchangeShop.shutdown()
    }
}
