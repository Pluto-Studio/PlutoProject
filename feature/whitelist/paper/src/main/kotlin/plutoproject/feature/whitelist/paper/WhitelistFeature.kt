package plutoproject.feature.whitelist.paper

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Server
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin
import org.koin.dsl.module
import plutoproject.capability.charonflow.api.CharonFlowConnection
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.feature.whitelist.api.WhitelistService
import plutoproject.feature.whitelist.common.commonModule
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.logging.Logger

@Feature(
    id = "whitelist",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo", "charonflow"],
)
@Suppress("UNUSED")
class WhitelistFeature : RuntimeModule {
    private var visitorListener: VisitorListener? = null
    private var restrictionListener: VisitorRestrictionListener? = null
    private var notificationHandler: NotificationHandler? = null

    override suspend fun onLoad(context: ModuleContext) {
        context as PaperModuleContext

        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<WhitelistConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<CharonFlowConnection>()
        context.loadKoinModuleDefinitions(
            module {
                single { config }
                single<CoroutineScope> { context.coroutineScope }
                single<Server> { context.plugin.server }
                single<Plugin> { context.plugin }
                single<Logger> { context.logger }
            },
            commonModule,
        )
        context.services.exportServiceFromKoin<WhitelistService>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext

        val server = context.plugin.server
        visitorListener = VisitorListener.also { server.pluginManager.registerEvents(it, context.plugin) }
        restrictionListener = VisitorRestrictionListener.also {
            server.pluginManager.registerEvents(it, context.plugin)
            it.startVisitorSpeedLimitationJob()
        }
        notificationHandler = NotificationHandler(
            context.services.getService<CharonFlowConnection>(),
            context.koinGet(),
            visitorListener!!,
        ).also { it.subscribe() }
    }

    override suspend fun onDisable(context: ModuleContext) {
        notificationHandler?.unsubscribe()
        notificationHandler = null
        restrictionListener?.stopVisitorSpeedLimitationJob()
        restrictionListener?.let(HandlerList::unregisterAll)
        restrictionListener = null
        visitorListener?.let(HandlerList::unregisterAll)
        visitorListener = null
    }
}
