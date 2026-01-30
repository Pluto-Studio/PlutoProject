package plutoproject.feature.common.whitelist_v2

import plutoproject.feature.common.api.whitelist_v2.VisitorRecord
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.*
import kotlin.time.Duration

class VisitorRecordImpl(
    override val uniqueId: UUID,
    override val ipAddress: InetAddress,
    override val virtualHost: InetSocketAddress,
    override val visitedAt: Instant,
    override val createdAt: Instant,
    override val duration: Duration,
    override val visitedServers: Collection<String>
) : VisitorRecord
