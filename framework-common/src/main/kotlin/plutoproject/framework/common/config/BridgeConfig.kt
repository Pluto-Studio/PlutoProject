package plutoproject.framework.common.config

import kotlin.time.Duration

data class BridgeConfig(
    val id: String = "plutoproject_server",
    val group: String? = null,
    val operationTimeout: Duration = Duration.parse("5s"),
    val debug: Boolean = false,
)
