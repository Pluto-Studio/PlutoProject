package ink.pmc.hypervisor

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.hypervisor.config.HypervisorConfig
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.platform.paper
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.chat.mochaYellow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object OverloadWarning : KoinComponent {
    private val config by inject<HypervisorConfig>()
    private var isRunning = false
    private var cycleJob: Job? = null

    fun start() {
        check(!isRunning) { "Overload warning job already running" }
        isRunning = true
        cycleJob = submitAsync {
            while (isRunning) {
                val millsPerTick = StatisticProvider.getMillsPerTick(MeasuringTime.SECONDS_10)
                if (millsPerTick != null && millsPerTick > 50) {
                    paper.broadcast(component {
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
                delay(config.overloadWarning.cyclePeriod)
            }
        }
    }

    fun stop() {
        check(isRunning) { "Overload warning job isn't running" }
        isRunning = false
        cycleJob?.cancel()
        cycleJob = null
    }
}