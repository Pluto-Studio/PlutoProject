package plutoproject.feature.paper.overloadWarning

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.chat.palettes.mochaYellow
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.api.statistic.MeasuringTime
import plutoproject.framework.paper.api.statistic.StatisticProvider
import plutoproject.framework.paper.util.server

@Feature(
    id = "overload_warning",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class OverloadWarningFeature : PaperFeature(), KoinComponent {
    private var isRunning: Boolean = false
    private var cycleJob: Job? = null
    private val config by inject<OverloadWarningConfig>()
    private val featureModule = module {
        single<OverloadWarningConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        start()
    }

    override fun onDisable() {
        stop()
    }

    private fun start() {
        check(!isRunning) { "Overload warning job already running" }
        isRunning = true
        cycleJob = PluginScope.launch {
            while (isRunning) {
                val millsPerTick = StatisticProvider.getMillsPerTick(MeasuringTime.SECONDS_10)
                if (millsPerTick != null && millsPerTick > 50) {
                    server.broadcast(component {
                        newline()
                        text("⚠ ") with mochaYellow
                        text("温馨提示 ") with mochaText
                        text("»") with mochaSubtext0
                        newline()
                        text("服务器目前处于严重过载状态，可能会出现生物停滞、不刷怪等现象") with mochaYellow
                        newline()
                        text("请关闭正在运行的机器、暂缓跑图") with mochaYellow
                        newline()
                        text("涉及到 TNT 的机器，需尽快关闭以避免损坏") with mochaYellow
                        newline()
                        text("稳定流畅的游戏体验需大家一同维护，感谢配合") with mochaYellow
                        newline()
                    })
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
