package ink.pmc.hypervisor.providers

import ink.pmc.hypervisor.MeasuringTime
import ink.pmc.hypervisor.MeasuringTime.*
import ink.pmc.hypervisor.StatisticProviderType
import me.lucko.spark.api.Spark
import me.lucko.spark.api.statistic.StatisticWindow

class SparkStatisticProvider(private val spark: Spark) : AbstractStatisticProvider() {
    override val type: StatisticProviderType = StatisticProviderType.SPARK

    override fun getTicksPerSecond(time: MeasuringTime): Double? {
        val statistic = spark.tps() ?: return null
        return statistic.poll(
            when (time) {
                SECONDS_10 -> StatisticWindow.TicksPerSecond.SECONDS_10
                MINUTE_1 -> StatisticWindow.TicksPerSecond.MINUTES_1
                MINUTES_5 -> StatisticWindow.TicksPerSecond.MINUTES_5
                MINUTES_10 -> StatisticWindow.TicksPerSecond.MINUTES_5
                MINUTES_15 -> StatisticWindow.TicksPerSecond.MINUTES_15
            }
        )
    }

    override fun getMillsPerTick(time: MeasuringTime): Double? {
        val statistic = spark.mspt() ?: return null
        return statistic.poll(
            when (time) {
                SECONDS_10 -> StatisticWindow.MillisPerTick.SECONDS_10
                MINUTE_1 -> StatisticWindow.MillisPerTick.MINUTES_1
                MINUTES_5 -> StatisticWindow.MillisPerTick.MINUTES_5
                MINUTES_10 -> StatisticWindow.MillisPerTick.MINUTES_5
                MINUTES_15 -> StatisticWindow.MillisPerTick.MINUTES_5
            }
        ).median()
    }

    override fun getCpuUsageSystem(time: MeasuringTime): Double {
        val statistic = spark.cpuSystem()
        return statistic.poll(
            when (time) {
                SECONDS_10 -> StatisticWindow.CpuUsage.SECONDS_10
                MINUTE_1 -> StatisticWindow.CpuUsage.MINUTES_1
                MINUTES_5 -> StatisticWindow.CpuUsage.MINUTES_1
                MINUTES_10 -> StatisticWindow.CpuUsage.MINUTES_1
                MINUTES_15 -> StatisticWindow.CpuUsage.MINUTES_15
            }
        )
    }

    override fun getCpuUsageProcess(time: MeasuringTime): Double {
        val statistic = spark.cpuProcess()
        return statistic.poll(
            when (time) {
                SECONDS_10 -> StatisticWindow.CpuUsage.SECONDS_10
                MINUTE_1 -> StatisticWindow.CpuUsage.MINUTES_1
                MINUTES_5 -> StatisticWindow.CpuUsage.MINUTES_1
                MINUTES_10 -> StatisticWindow.CpuUsage.MINUTES_1
                MINUTES_15 -> StatisticWindow.CpuUsage.MINUTES_15
            }
        )
    }
}