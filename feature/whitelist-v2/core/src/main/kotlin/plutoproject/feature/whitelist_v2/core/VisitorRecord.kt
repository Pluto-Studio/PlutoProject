package plutoproject.feature.whitelist_v2.core

import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

data class VisitorRecord(
    val uniqueId: UUID,
    val ipAddress: InetAddress,
    val virtualHost: InetSocketAddress,
    val visitedAt: Instant,
    val createdAt: Instant,
    val duration: Duration,
    val visitedServers: Collection<String>,
)
