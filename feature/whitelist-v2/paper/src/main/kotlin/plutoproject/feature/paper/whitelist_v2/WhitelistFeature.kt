package plutoproject.feature.paper.whitelist_v2

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.whitelist_v2.adapter.KnownVisitors
import plutoproject.feature.whitelist_v2.adapter.WhitelistService
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.core.VisitorRecordRepository
import plutoproject.feature.whitelist_v2.core.WhitelistCore
import plutoproject.feature.whitelist_v2.core.WhitelistRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.MongoVisitorRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.MongoWhitelistRecordRepository
import plutoproject.feature.whitelist_v2.infra.mongo.model.VisitorRecordDocument
import plutoproject.feature.whitelist_v2.infra.mongo.model.WhitelistRecordDocument
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
        single { WhitelistCore(get(), get(), get()) }
        single<Whitelist> { WhitelistService(get(), get()) }
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
