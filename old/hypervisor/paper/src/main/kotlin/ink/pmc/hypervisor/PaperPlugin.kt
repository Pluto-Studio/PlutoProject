package ink.pmc.hypervisor

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.PropertySource
import ink.pmc.framework.options.OptionsManager
import ink.pmc.framework.config.preconfiguredConfigLoaderBuilder
import ink.pmc.framework.inject.modifyExistedKoinOrCreate
import ink.pmc.framework.jvm.findClass
import ink.pmc.framework.storage.saveResourceIfNotExisted
import ink.pmc.hypervisor.StatisticProviderType.NATIVE
import ink.pmc.hypervisor.StatisticProviderType.SPARK
import ink.pmc.hypervisor.button.VIEW_BOOST_BUTTON_DESCRIPTOR
import ink.pmc.hypervisor.button.ViewBoost
import ink.pmc.hypervisor.commands.DynamicSchedulingCommand
import ink.pmc.hypervisor.commands.StatusCommand
import ink.pmc.hypervisor.config.HypervisorConfig
import ink.pmc.hypervisor.listeners.DynamicViewDistanceListener
import ink.pmc.hypervisor.listeners.StatusCommandListener
import ink.pmc.hypervisor.providers.NativeStatisticProvider
import ink.pmc.hypervisor.providers.SparkStatisticProvider
import ink.pmc.menu.api.MenuManager
import ink.pmc.menu.api.isMenuAvailable
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import java.util.logging.Logger

lateinit var commandManager: LegacyPaperCommandManager<CommandSender>
lateinit var annotationParser: AnnotationParser<CommandSender>
lateinit var plugin: JavaPlugin
lateinit var pluginLogger: Logger
var disabled = true

@Suppress("UNUSED")
class PaperPlugin : SuspendingJavaPlugin(), KoinComponent {
    private val config by inject<HypervisorConfig>()
    private val bukkitModule = module {
        single<HypervisorConfig> {
            preconfiguredConfigLoaderBuilder()
                .addPropertySource(PropertySource.file(saveResourceIfNotExisted("config.conf")))
                .build()
                .loadConfigOrThrow()
        }
        single<StatisticProvider> {
            when (config.statisticProvider) {
                NATIVE -> NativeStatisticProvider()
                SPARK -> if (findClass("me.lucko.spark.api.Spark") != null) {
                    SparkStatisticProvider(SparkHook.instance)
                } else {
                    logger.warn("Spark not found, fallback to native statistic provider")
                    NativeStatisticProvider()
                }
            }
        }
        single<DynamicScheduling> { DynamicSchedulingImpl() }
    }

    override fun onEnable() {
        plugin = this
        pluginLogger = logger

        modifyExistedKoinOrCreate {
            modules(bukkitModule)
        }

        logger.info("Using statistic provider: ${StatisticProvider.type}")

        commandManager = LegacyPaperCommandManager.createNative(
            this,
            ExecutionCoordinator.asyncCoordinator()
        )
        commandManager.registerBrigadier()

        annotationParser = AnnotationParser(commandManager, CommandSender::class.java)
        annotationParser.installCoroutineSupport()
        annotationParser.parse(DynamicSchedulingCommand)

        if (isMenuAvailable) {
            MenuManager.registerButton(VIEW_BOOST_BUTTON_DESCRIPTOR) { ViewBoost() }
        }
        OptionsManager.registerOptionDescriptor(DYNAMIC_VIEW_DISTANCE)

        if (config.dynamicScheduling.enabled) {
            server.pluginManager.registerSuspendingEvents(DynamicViewDistanceListener, this)
            DynamicScheduling.start()
        }
        if (config.overloadWarning.enabled) {
            OverloadWarning.start()
        }
        if (config.statusCommand.enabled) {
            annotationParser.parse(StatusCommand)
            server.pluginManager.registerSuspendingEvents(StatusCommandListener, this)
        }

        disabled = false
    }

    override fun onDisable() {
        if (config.dynamicScheduling.enabled) {
            DynamicScheduling.stop()
        }
        if (config.overloadWarning.enabled) {
            OverloadWarning.stop()
        }

        disabled = true
    }

}