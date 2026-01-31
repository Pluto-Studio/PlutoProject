package plutoproject.feature.paper.whitelist_v2

import club.plutoproject.charonflow.CharonFlow
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.common.whitelist_v2.VisitorNotification
import plutoproject.feature.common.whitelist_v2.whitelistCommonModule
import plutoproject.framework.common.api.connection.CharonFlowConnection
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Dependency
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.common.util.jvm.findClass
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import java.util.logging.Logger

internal lateinit var featureLogger: Logger

@Feature(
    id = "whitelist_v2",
    platform = Platform.PAPER,
    dependencies = [Dependency("warp", required = false)]
)
@Suppress("UNUSED")
class WhitelistFeature : PaperFeature(), KoinComponent {
    private val config by inject<WhitelistConfig>()
    private val featureModule = module {
        single<WhitelistConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule, whitelistCommonModule)
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
}
