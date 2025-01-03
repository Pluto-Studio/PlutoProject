package ink.pmc.hypervisor.providers

import ink.pmc.hypervisor.MeasuringTime
import ink.pmc.hypervisor.MeasuringTime.*
import ink.pmc.hypervisor.StatisticProviderType
import ink.pmc.framework.platform.internal
import ink.pmc.framework.platform.paper
import org.bukkit.Bukkit

class NativeStatisticProvider : AbstractStatisticProvider() {
    override val type: StatisticProviderType = StatisticProviderType.NATIVE

    override fun getTicksPerSecond(time: MeasuringTime): Double? {
        val tps = Bukkit.getServer().tps
        return when (time) {
            SECONDS_10 -> tps.getOrNull(0)
            MINUTE_1 -> tps.getOrNull(0)
            MINUTES_5 -> tps.getOrNull(1)
            MINUTES_10 -> tps.getOrNull(1)
            MINUTES_15 -> tps.getOrNull(2)
        }
    }

    override fun getMillsPerTick(time: MeasuringTime): Double {
        return when (time) {
            SECONDS_10 -> paper.internal.tickTimes10s.average
            MINUTE_1 -> paper.internal.tickTimes60s.average
            MINUTES_5 -> paper.internal.tickTimes60s.average
            MINUTES_10 -> paper.internal.tickTimes60s.average
            MINUTES_15 -> paper.internal.tickTimes60s.average
        }
    }

    override fun getCpuUsageSystem(time: MeasuringTime): Double {
        throw UnsupportedOperationException("Unsupported")
    }

    override fun getCpuUsageProcess(time: MeasuringTime): Double {
        throw UnsupportedOperationException("Unsupported")
    }
}