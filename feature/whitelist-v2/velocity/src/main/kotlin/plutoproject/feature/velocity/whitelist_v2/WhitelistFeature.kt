package plutoproject.feature.velocity.whitelist_v2

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.github.shynixn.mccoroutine.velocity.registerSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.luckperms.api.LuckPermsProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.velocity.whitelist_v2.commands.MigratorCommand
import plutoproject.feature.velocity.whitelist_v2.commands.WhitelistCommand
import plutoproject.feature.velocity.whitelist_v2.commands.WhitelistVisitorCommand
import plutoproject.feature.velocity.whitelist_v2.listeners.PlayerListener
import plutoproject.feature.velocity.whitelist_v2.listeners.VisitorListener
import plutoproject.feature.whitelist_v2.adapter.KnownVisitors
import plutoproject.feature.whitelist_v2.adapter.WhitelistService
import plutoproject.feature.whitelist_v2.api.Whitelist
import plutoproject.feature.whitelist_v2.api.hook.WhitelistHookType
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
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.velocity.api.feature.VelocityFeature
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server
import java.time.Clock
import java.util.logging.Logger

private const val WHITELIST_PREFIX = "whitelist_v2_"
private const val WHITELIST_RECORD_COLLECTION = "whitelist_records"
private const val VISITOR_RECORD_COLLECTION = "visitor_records"

lateinit var featureLogger: Logger

@Feature(
    id = "whitelist_v2",
    platform = Platform.VELOCITY,
)
@Suppress("UNUSED")
class WhitelistFeature : VelocityFeature(), KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val whitelist by inject<Whitelist>()

    private val configModule = module {
        single<WhitelistConfig> { loadConfig(saveConfig()) }
    }

    private val featureModule = module {
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
        // 先只依赖注入 Config，用于下面的 LuckPerms API 检测
        configureKoin {
            modules(configModule)
        }

        val isLuckPermsApiPresent = runCatching { LuckPermsProvider.get() }.isSuccess
        if (config.visitorMode.enable && !isLuckPermsApiPresent) {
            logger.severe("访客模式功能已启用，但未找到 LuckPerms API。模块将不会加载。")
            return
        }

        configureKoin {
            modules(featureModule)
        }

        VisitorState.setEnabled(config.visitorMode.enable)
        featureLogger = logger
        registerCommands()
        server.eventManager.registerSuspend(plugin, PlayerListener)
        server.eventManager.registerSuspend(plugin, VisitorListener)

        whitelist.registerHook(WhitelistHookType.GrantWhitelist, ::onWhitelistGrant)
        whitelist.registerHook(WhitelistHookType.RevokeWhitelist, ::onWhitelistRevoke)
    }

    private fun registerCommands() {
        AnnotationParser.parse(WhitelistCommand)
        AnnotationParser.parse(WhitelistVisitorCommand)
        if (config.enableMigrator) {
            AnnotationParser.parse(MigratorCommand)
        }
    }

    private inline fun <reified T : Any> getCollection(name: String): MongoCollection<T> {
        return MongoConnection.getCollection("$WHITELIST_PREFIX$name")
    }
}
