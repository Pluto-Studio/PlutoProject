package plutoproject.feature.overload_warning.paper

import kotlin.time.Duration

data class OverloadWarningConfig(
    val cyclePeriod: Duration = Duration.parse("5m")
)
