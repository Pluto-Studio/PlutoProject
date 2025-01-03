package ink.pmc.hypervisor.config

import ink.pmc.hypervisor.StatisticProviderType

data class HypervisorConfig(
    val statisticProvider: StatisticProviderType,
    val statusCommand: StatusCommand,
    val dynamicScheduling: DynamicScheduling,
    val overloadWarning: OverloadWarning
)