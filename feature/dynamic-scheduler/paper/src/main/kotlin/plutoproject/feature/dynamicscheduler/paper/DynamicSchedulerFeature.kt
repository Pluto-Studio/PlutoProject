package plutoproject.feature.dynamicscheduler.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.serverstatistics.api.statistic.StatisticProvider
import plutoproject.feature.dynamicscheduler.api.paper.DynamicScheduler
import plutoproject.feature.dynamicscheduler.paper.buttons.ViewBoost
import plutoproject.feature.dynamicscheduler.paper.buttons.ViewBoostButtonDescriptor
import plutoproject.feature.dynamicscheduler.paper.commands.DynamicSchedulerCommand
import plutoproject.feature.dynamicscheduler.paper.config.DynamicSchedulerConfig
import plutoproject.feature.dynamicscheduler.paper.listeners.DynamicViewDistanceListener
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.logging.Logger

var disabled = true
internal lateinit var featureLogger: Logger

@Feature(
    id = "dynamic_scheduler",
    platform = Platform.PAPER,
    optionalFeatures = ["menu"],
    requiredCapabilities = ["database_persist", "server_statistics", "interactive", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
class DynamicSchedulerFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(DynamicSchedulerFeature::class.java.classLoader)
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<DynamicSchedulerConfig>()
        context.importServiceToKoin<DatabasePersist>()
        context.importServiceToKoin<StatisticProvider>()
        context.importServiceToKoin<GuiManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<DynamicScheduler> { DynamicSchedulerImpl() }
        })
        context.services.exportServiceFromKoin<DynamicScheduler>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        val paper = context as PaperModuleContext
        featureLogger = context.logger
        context.logger.info("Using statistic provider: ${context.koinGet<StatisticProvider>().type}")
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, DynamicSchedulerCommand)
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(ViewBoostButtonDescriptor) { ViewBoost() }
        paper.plugin.server.pluginManager.registerSuspendingEvents(DynamicViewDistanceListener, paper.plugin)
        context.koinGet<DynamicScheduler>().start()
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        HandlerList.unregisterAll(DynamicViewDistanceListener)
        context.koinGet<DynamicScheduler>().stop()
        disabled = true
    }
}
