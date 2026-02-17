package plutoproject.feature.whitelist_v2.core

import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import kotlin.time.Duration

data class VisitorRecordParams(
    val ipAddress: InetAddress,
    val virtualHost: InetSocketAddress,
    val visitedAt: Instant,
    val duration: Duration,
    val visitedServers: Collection<String>,
)
