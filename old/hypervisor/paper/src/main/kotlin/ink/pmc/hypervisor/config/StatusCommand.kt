package ink.pmc.hypervisor.config

data class StatusCommand(
    val enabled: Boolean,
    val overrideTpsCommand: Boolean,
    val overrideMsptCommand: Boolean
)