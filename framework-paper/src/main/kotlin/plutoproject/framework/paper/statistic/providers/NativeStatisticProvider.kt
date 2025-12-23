package plutoproject.framework.paper.statistic.providers

import org.bukkit.Bukkit
import plutoproject.framework.paper.api.statistic.MeasuringTime
import plutoproject.framework.paper.api.statistic.MeasuringTime.*
import plutoproject.framework.paper.api.statistic.StatisticProviderType

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
        error("NativeStatisticProvider not available.")
        /*
        return when (time) {
            SECONDS_10 -> server.toNms().tickTimes10s.average
            MINUTE_1 -> server.toNms().tickTimes60s.average
            MINUTES_5 -> server.toNms().tickTimes60s.average
            MINUTES_10 -> server.toNms().tickTimes60s.average
            MINUTES_15 -> server.toNms().tickTimes60s.average
        }
        */
    }

    override fun getCpuUsageSystem(time: MeasuringTime): Double {
        throw UnsupportedOperationException("Unsupported")
    }

    override fun getCpuUsageProcess(time: MeasuringTime): Double {
        throw UnsupportedOperationException("Unsupported")
    }
}
