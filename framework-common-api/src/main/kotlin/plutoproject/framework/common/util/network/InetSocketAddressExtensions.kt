package plutoproject.framework.common.util.network

import java.net.InetSocketAddress

fun InetSocketAddress.toHostPortString(): String =
    "${hostString}:${port}"

/**
 * 从指定的字符串解析 [InetSocketAddress]。
 *
 * @param defaultPort 当字符串中找不到端口时使用的端口
 * @return 解析出的 [InetSocketAddress]
 */
fun String.parseInetSocketAddress(defaultPort: Int = 0): InetSocketAddress {
    val parts = split(":")
    val host = parts[0]
    val port = if (parts.size > 1) parts[1].toIntOrNull() ?: defaultPort else defaultPort
    return InetSocketAddress(host, port)
}
