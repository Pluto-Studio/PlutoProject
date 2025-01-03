package ink.pmc.daily

data class DailyConfig(
    val rewards: Rewards,
)

data class Rewards(
    val weekday: Double = 5.0,
    val weekend: Double = 10.0,
    val accumulate: Double = 10.0,
    val accumulateRequirement: Int = 7,
    val holiday: Double = 20.0,
)