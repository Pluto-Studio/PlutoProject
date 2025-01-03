package ink.pmc.hypervisor.providers

import ink.pmc.hypervisor.LoadLevel
import ink.pmc.hypervisor.MeasuringTime
import ink.pmc.hypervisor.StatisticProvider

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