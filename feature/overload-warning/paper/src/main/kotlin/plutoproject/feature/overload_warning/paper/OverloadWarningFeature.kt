package plutoproject.feature.overload_warning.paper

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.dsl.module
import plutoproject.capability.serverstatistics.api.statistic.MeasuringTime
import plutoproject.capability.serverstatistics.api.statistic.StatisticProvider
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "overload_warning",
    platform = Platform.PAPER,
    requiredCapabilities = ["server_statistics"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class OverloadWarningFeature : RuntimeModule {
    private var isRunning: Boolean = false
    private var cycleJob: Job? = null
    private val config by koinInject<OverloadWarningConfig>()
    private val statistics by koinInject<StatisticProvider>()

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val loadedConfig = ConfigLoaderBuilder.empty()
            .withClassLoader(OverloadWarningFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<OverloadWarningConfig>()
        context.loadKoinModuleDefinitions(module { single { loadedConfig } })
        context.importServiceToKoin<StatisticProvider>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        start()
    }

    override suspend fun onDisable(context: ModuleContext) {
        stop()
    }

    private fun start() {
        check(!isRunning) { "Overload warning job already running" }
        isRunning = true
        val context = currentModuleContext()
        val server = (context as PaperModuleContext).plugin.server
        cycleJob = context.coroutineScope.launch {
            while (isRunning) {
                val millsPerTick = statistics.getMillsPerTick(MeasuringTime.SECONDS_10)
                if (millsPerTick != null && millsPerTick > 50) {
                    server.broadcast(OVERLOAD_WARNING)
                }
                delay(config.cyclePeriod)
            }
        }
    }

    private fun stop() {
        check(isRunning) { "Overload warning job isn't running" }
        isRunning = false
        cycleJob?.cancel()
        cycleJob = null
    }
}
