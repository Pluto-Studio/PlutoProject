package plutoproject.framework.paper.api.statistic

import plutoproject.framework.common.util.inject.globalKoin

interface StatisticProvider {
    companion object : StatisticProvider by globalKoin.get()

    val type: StatisticProviderType

    fun getLoadLevel(): LoadLevel?

    fun getTicksPerSecond(time: MeasuringTime): Double?

    fun getMillsPerTick(time: MeasuringTime): Double?

    fun getCpuUsageSystem(time: MeasuringTime): Double?

    fun getCpuUsageProcess(time: MeasuringTime): Double?
}
