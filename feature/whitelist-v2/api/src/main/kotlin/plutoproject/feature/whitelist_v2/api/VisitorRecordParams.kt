package plutoproject.feature.whitelist_v2.api

import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import kotlin.time.Duration

/**
 * 代表创建访客记录时需要提供的信息。
 */
data class VisitorRecordParams(
    /**
     * 该访客的 IP 地址。
     */
    val ipAddress: InetAddress,

    /**
     * 该访客连接到服务器时的连接地址。
     */
    val virtualHost: InetSocketAddress,

    /**
     * 该访客本次来访（即进入游戏）的时间。
     */
    val visitedAt: Instant,

    /**
     * 该访客在服务器停留的时间。
     */
    val duration: Duration,

    /**
     * 该访客访问过的子服，存储为代理端上配置的服务器 ID。
     */
    val visitedServers: Collection<String>,
)
