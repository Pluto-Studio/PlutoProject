package plutoproject.feature.daily.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.feature.daily.api.paper.Daily
import plutoproject.feature.daily.paper.buttons.Daily as DailyButton
import plutoproject.feature.daily.paper.buttons.DailyButtonDescriptor
import plutoproject.feature.daily.paper.commands.CheckInCommand
import plutoproject.feature.daily.paper.commands.DailyCalendarCommand
import plutoproject.feature.daily.paper.listeners.PlayerListener
import plutoproject.feature.daily.paper.repositories.DailyHistoryRepository
import plutoproject.feature.daily.paper.repositories.DailyUserRepository
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.logging.Logger

private const val COLLECTION_PREFIX = "plutoproject_feature_daily_"
internal lateinit var featureLogger: Logger

@Feature(
    id = "daily",
    platform = Platform.PAPER,
    optionalFeatures = ["menu"],
    requiredCapabilities = ["mongo", "database_persist", "interactive", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class DailyFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(DailyFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<DailyConfig>()
        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<DatabasePersist>()
        context.importServiceToKoin<GuiManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<Daily> { DailyImpl() }
            single { DailyUserRepository(get<MongoConnection>().getCollection("${COLLECTION_PREFIX}user")) }
            single { DailyHistoryRepository(get<MongoConnection>().getCollection("${COLLECTION_PREFIX}history")) }
        })
        context.services.exportServiceFromKoin<Daily>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        val paper = context as PaperModuleContext
        featureLogger = context.logger
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, CheckInCommand, DailyCalendarCommand)
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(DailyButtonDescriptor) { DailyButton() }
        paper.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, paper.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(PlayerListener)
        context.koinGet<Daily>().shutdown()
    }
}
