package plutoproject.feature.whitelist_v2.adapter.common.impl

import plutoproject.feature.whitelist_v2.api.VisitorRecordInfo
import plutoproject.feature.whitelist_v2.core.VisitorRecord
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

class VisitorRecordInfoImpl(
    private val data: VisitorRecord,
) : VisitorRecordInfo {
    override val uniqueId: UUID
        get() = data.uniqueId

    override val ipAddress: InetAddress
        get() = data.ipAddress

    override val virtualHost: InetSocketAddress
        get() = data.virtualHost

    override val visitedAt: Instant
        get() = data.visitedAt

    override val createdAt: Instant
        get() = data.createdAt

    override val duration: Duration
        get() = data.duration

    override val visitedServers: Collection<String>
        get() = data.visitedServers
}
