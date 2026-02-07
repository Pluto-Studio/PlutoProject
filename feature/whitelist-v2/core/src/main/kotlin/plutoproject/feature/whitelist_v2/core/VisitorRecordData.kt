package plutoproject.feature.whitelist_v2.core

import plutoproject.feature.whitelist_v2.api.VisitorRecord
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

data class VisitorRecordData(
    override val uniqueId: UUID,
    override val ipAddress: InetAddress,
    override val virtualHost: InetSocketAddress,
    override val visitedAt: Instant,
    override val createdAt: Instant,
    override val duration: Duration,
    override val visitedServers: Collection<String>,
) : VisitorRecord
