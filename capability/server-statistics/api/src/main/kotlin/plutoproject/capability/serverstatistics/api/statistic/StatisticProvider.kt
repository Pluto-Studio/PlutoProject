package plutoproject.capability.serverstatistics.api.statistic

interface StatisticProvider {
    val type: StatisticProviderType

    fun getLoadLevel(): LoadLevel?

    fun getTicksPerSecond(time: MeasuringTime): Double?

    fun getMillsPerTick(time: MeasuringTime): Double?

    fun getCpuUsageSystem(time: MeasuringTime): Double?

    fun getCpuUsageProcess(time: MeasuringTime): Double?
}
