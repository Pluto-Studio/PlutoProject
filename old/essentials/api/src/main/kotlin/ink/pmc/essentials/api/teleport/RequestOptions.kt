package ink.pmc.essentials.api.teleport

import kotlin.time.Duration

@Suppress("UNUSED")
data class RequestOptions(
    val expireAfter: Duration,
    val removeAfter: Duration
)