package plutoproject.capability.serverstatistics.paper.providers

import plutoproject.capability.serverstatistics.api.statistic.LoadLevel
import plutoproject.capability.serverstatistics.api.statistic.MeasuringTime
import plutoproject.capability.serverstatistics.api.statistic.StatisticProvider

abstract class AbstractStatisticProvider : StatisticProvider {
    override fun getLoadLevel(): LoadLevel? {
        val millsPerTick = getMillsPerTick(MeasuringTime.SECONDS_10) ?: return null
        return when {
            millsPerTick < 25.0 -> LoadLevel.LOW
            millsPerTick in 25.0..50.0 -> LoadLevel.MODERATE
            millsPerTick > 50 -> LoadLevel.HIGH
            else -> error("Unreachable")
        }
    }
}
