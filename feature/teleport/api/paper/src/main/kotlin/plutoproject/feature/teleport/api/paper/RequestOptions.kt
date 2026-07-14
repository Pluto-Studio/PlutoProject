package plutoproject.feature.teleport.api.paper

import kotlin.time.Duration

@Suppress("UNUSED")
data class RequestOptions(
    val expireAfter: Duration,
    val removeAfter: Duration
)
