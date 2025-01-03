package ink.pmc.hypervisor

import ink.pmc.framework.inject.inlinedGet

interface StatisticProvider {
    companion object : StatisticProvider by inlinedGet()

    val type: StatisticProviderType

    fun getLoadLevel(): LoadLevel?

    fun getTicksPerSecond(time: MeasuringTime): Double?

    fun getMillsPerTick(time: MeasuringTime): Double?

    fun getCpuUsageSystem(time: MeasuringTime): Double?

    fun getCpuUsageProcess(time: MeasuringTime): Double?
}