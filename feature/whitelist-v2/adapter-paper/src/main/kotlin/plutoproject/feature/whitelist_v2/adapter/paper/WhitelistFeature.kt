package plutoproject.feature.whitelist_v2.adapter.paper

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.whitelist_v2.adapter.common.KnownVisitors
import plutoproject.feature.whitelist_v2.adapter.common.WhitelistService
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.feature.whitelist_v2.core.usecase.CreateVisitorRecordUseCase
import plutoproject.feature.whitelist_v2.core.usecase.GrantWhitelistUseCase
import plutoproject.feature.whitelist_v2.core.usecase.IsWhitelistedUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupVisitorRecordUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupVisitorRecordsByCidrUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupVisitorRecordsByIpUseCase
import plutoproject.feature.whitelist_v2.core.usecase.LookupWhitelistRecordUseCase
import plutoproject.feature.whitelist_v2.core.usecase.RevokeWhitelistUseCase
import plutoproject.feature.whitelist_v2.infra.mongo.MongoVisitorRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.MongoWhitelistRecordRepository
import plutoproject.framework.common.api.connection.MongoConnection
import plutoproject.framework.common.api.connection.getCollection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.time.Clock
import java.util.logging.Logger

private const val WHITELIST_PREFIX = "whitelist_v2_"
private const val WHITELIST_RECORD_COLLECTION = "whitelist_records"
private const val VISITOR_RECORD_COLLECTION = "visitor_records"

internal lateinit var featureLogger: Logger

@Feature(
    id = "whitelist_v2",
    platform = Platform.PAPER,
    dependencies = [Dependency("warp", required = false)],
)
@Suppress("UNUSED")
class WhitelistFeature : PaperFeature(), KoinComponent {
    private val config by inject<WhitelistConfig>()

    private val featureModule = module {
        single<WhitelistConfig> { loadConfig(saveConfig()) }

        single { Clock.systemUTC() }
        single { KnownVisitors() }

        single<WhitelistRecordRepository> {
            MongoWhitelistRecordRepository(getCollection(WHITELIST_RECORD_COLLECTION))
        }
        single<VisitorRecordRepository> {
            val repo = MongoVisitorRecordRepository(getCollection(VISITOR_RECORD_COLLECTION))
            PluginScope.launch(Dispatchers.IO) {
                repo.ensureIndexes()
            }
            repo
        }

        single { IsWhitelistedUseCase(get()) }
        single { LookupWhitelistRecordUseCase(get()) }
        single { GrantWhitelistUseCase(get(), get(), get()) }
        single { RevokeWhitelistUseCase(get(), get()) }
        single { LookupVisitorRecordUseCase(get()) }
        single { CreateVisitorRecordUseCase(get(), get()) }
        single { LookupVisitorRecordsByCidrUseCase(get()) }
        single { LookupVisitorRecordsByIpUseCase(get()) }

        single<Whitelist> { WhitelistService(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        featureLogger = logger
        registerListeners()
        VisitorRestrictionListener.startVisitorSpeedLimitationJob()
        runBlocking {
            subscribeNotificationTopic()
        }
    }

    private fun registerListeners() {
        server.pluginManager.registerSuspendingEvents(VisitorListener, plugin)
        server.pluginManager.registerSuspendingEvents(VisitorRestrictionListener, plugin)
    }

    override fun onDisable() {
        VisitorRestrictionListener.stopVisitorSpeedLimitationJob()
        runBlocking {
            unsubscribeNotificationTopic()
        }
    }

    private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
        return MongoConnection.getCollection("$WHITELIST_PREFIX$name")
    }
}
