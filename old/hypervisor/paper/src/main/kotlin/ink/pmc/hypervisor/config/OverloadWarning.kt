package ink.pmc.hypervisor.config

import kotlin.time.Duration

data class OverloadWarning(
    val enabled: Boolean,
    val cyclePeriod: Duration
)