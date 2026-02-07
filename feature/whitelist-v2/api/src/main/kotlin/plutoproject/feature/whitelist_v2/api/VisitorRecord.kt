package plutoproject.feature.whitelist_v2.api

import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

/**
 * 代表一次访客会话记录。
 *
 * 每位以访客身份连接到服务器的玩家，在退出时都会创建一份记录。
 */
interface VisitorRecord {
    /**
     * 该访客记录的玩家 UUID。
     */
    val uniqueId: UUID

    /**
     * 该访客的 IP 地址。
     */
    val ipAddress: InetAddress

    /**
     * 该访客连接到服务器时的连接地址。
     */
    val virtualHost: InetSocketAddress

    /**
     * 该访客本次来访（即进入游戏）的时间。
     */
    val visitedAt: Instant

    /**
     * 该访客记录创建（即访客退出服务器）的时间。
     */
    val createdAt: Instant

    /**
     * 该访客在服务器停留的时间。
     */
    val duration: Duration

    /**
     * 该访客访问过的子服，存储为代理端上配置的服务器 ID。
     */
    val visitedServers: Collection<String>
}
