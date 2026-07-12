package plutoproject.capability.serverstatistics.paper

import me.lucko.spark.api.SparkProvider
import org.bukkit.Bukkit
import org.koin.dsl.module
import plutoproject.capability.serverstatistics.api.statistic.StatisticProvider
import plutoproject.capability.serverstatistics.paper.providers.NativeStatisticProvider
import plutoproject.capability.serverstatistics.paper.providers.SparkStatisticProvider
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportServiceFromKoin
import plutoproject.kernel.api.loadKoinModuleDefinitions

@Capability(id = "server-statistics", platform = Platform.PAPER)
class PaperServerStatisticsCapability : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        val provider = if (Bukkit.getPluginManager().getPlugin("spark") == null) {
            NativeStatisticProvider()
        } else {
            SparkStatisticProvider(SparkProvider.get())
        }
        context.loadKoinModuleDefinitions(module {
            single<StatisticProvider> { provider }
        })
        context.services.exportServiceFromKoin<StatisticProvider>()
    }
}
