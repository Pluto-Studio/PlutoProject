package plutoproject.feature.whitelist.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
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
    optionalFeatures = ["warp"],
    requiredCapabilities = ["mongo", "charonflow"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class WhitelistFeature : RuntimeModule {
    private var visitorListener: VisitorListener? = null
    private var restrictionListener: VisitorRestrictionListener? = null
    private var notificationHandler: NotificationHandler? = null

    override suspend fun onLoad(context: ModuleContext) {
        context as PaperModuleContext

        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(WhitelistFeature::class.java.classLoader)
            .withExplicitSealedTypes()
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
                single<Logger> { context.logger }
            },
            commonModule(context.coroutineScope),
        )
        context.services.exportServiceFromKoin<WhitelistService>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext

        val server = context.plugin.server
        visitorListener = VisitorListener.also {
            server.pluginManager.registerSuspendingEvents(it, context.plugin)
        }
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
