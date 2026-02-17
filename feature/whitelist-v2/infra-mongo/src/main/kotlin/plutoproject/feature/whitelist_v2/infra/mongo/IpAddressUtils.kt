package plutoproject.feature.whitelist_v2.infra.mongo

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

internal fun InetAddress.toLongs(): Pair<Long, Long> {
    return when (this) {
        is Inet4Address -> {
            val bytes = address
            val low = ((bytes[0].toLong() and 0xFF) shl 24) or
                ((bytes[1].toLong() and 0xFF) shl 16) or
                ((bytes[2].toLong() and 0xFF) shl 8) or
                (bytes[3].toLong() and 0xFF)
            0L to low
        }

        is Inet6Address -> {
            val bytes = address
            val high = bytesToLong(bytes, 0)
            val low = bytesToLong(bytes, 8)
            high to low
        }

        else -> throw IllegalArgumentException("Unsupported address type: $javaClass")
    }
}

private fun bytesToLong(bytes: ByteArray, offset: Int): Long {
    return ((bytes[offset].toLong() and 0xFF) shl 56) or
        ((bytes[offset + 1].toLong() and 0xFF) shl 48) or
        ((bytes[offset + 2].toLong() and 0xFF) shl 40) or
        ((bytes[offset + 3].toLong() and 0xFF) shl 32) or
        ((bytes[offset + 4].toLong() and 0xFF) shl 24) or
        ((bytes[offset + 5].toLong() and 0xFF) shl 16) or
        ((bytes[offset + 6].toLong() and 0xFF) shl 8) or
        (bytes[offset + 7].toLong() and 0xFF)
}

internal fun String.toIpRange(): IpRange {
    val parts = split("/")
    require(parts.size == 2) { "Invalid CIDR format: $this" }

    val ipPart = parts[0]
    val prefix = parts[1].toInt()

    // IPv4-mapped IPv6 addresses contain '.' as well (e.g. ::ffff:192.0.2.128),
    // so we must prefer ':' detection for IPv6.
    return if (ipPart.contains(":")) {
        parseIpv6Cidr(ipPart, prefix)
    } else {
        parseIpv4Cidr(ipPart, prefix)
    }
}

private fun parseIpv4Cidr(ip: String, prefix: Int): IpRange.Ipv4 {
    require(prefix in 0..32) { "IPv4 prefix must be between 0 and 32, got: $prefix" }

    val ipLong = ip.ipv4ToLong()
    return if (prefix == 0) {
        IpRange.Ipv4(0L, 0xFFFFFFFFL)
    } else {
        val mask = (-1L shl (32 - prefix)) and 0xFFFFFFFFL
        val start = ipLong and mask
        val end = start or (mask.inv() and 0xFFFFFFFFL)
        IpRange.Ipv4(start, end)
    }
}

private fun parseIpv6Cidr(ip: String, prefix: Int): IpRange.Ipv6 {
    require(prefix in 0..128) { "IPv6 prefix must be between 0 and 128, got: $prefix" }

    val (high, low) = ip.ipv6ToLongs()

    return when {
        prefix == 0 -> IpRange.Ipv6(0L, 0L, -1L, -1L)
        prefix < 64 -> {
            val shift = 64 - prefix
            val mask = -1L shl shift
            val startHigh = high and mask
            val endHigh = startHigh or mask.inv()
            IpRange.Ipv6(startHigh, 0L, endHigh, -1L)
        }

        prefix == 64 -> IpRange.Ipv6(high, 0L, high, -1L)

        else -> {
            val shift = 128 - prefix
            val mask = -1L shl shift
            val startLow = low and mask
            val endLow = startLow or mask.inv()
            IpRange.Ipv6(high, startLow, high, endLow)
        }
    }
}

private fun String.ipv4ToLong(): Long {
    val parts = split(".")
    require(parts.size == 4) { "Invalid IPv4 address: $this" }

    return parts.fold(0L) { acc, part ->
        val octet = part.toInt()
        require(octet in 0..255) { "Invalid octet in IPv4 address: $part" }
        (acc shl 8) or octet.toLong()
    }
}

private fun String.ipv6ToLongs(): Pair<Long, Long> {
    val expanded = expandIPv6()
    val parts = expanded.split(":")
    require(parts.size == 8) { "Invalid IPv6 address: $this" }

    val high = parts.take(4).fold(0L) { acc, part ->
        (acc shl 16) or part.toLong(16)
    }
    val low = parts.drop(4).fold(0L) { acc, part ->
        (acc shl 16) or part.toLong(16)
    }

    return high to low
}

private fun String.expandIPv6(): String {
    if (contains(".")) {
        val ipv4Part = substringAfterLast(":")
        val ipv4Segments = ipv4Part.split(".").map { it.toInt() }
        require(ipv4Segments.size == 4) { "Invalid IPv4-mapped IPv6 address: $this" }

        val segment1 = (ipv4Segments[0] shl 8) or ipv4Segments[1]
        val segment2 = (ipv4Segments[2] shl 8) or ipv4Segments[3]

        val ipv6Part = substringBeforeLast(":")
        val mappedPart = "${segment1.toString(16).padStart(4, '0')}:${segment2.toString(16).padStart(4, '0')}"
        val fullAddress = if (ipv6Part.endsWith(":")) "$ipv6Part$mappedPart" else "$ipv6Part:$mappedPart"
        return fullAddress.expandIPv6()
    }

    if (contains("::")) {
        val splitParts = split("::", limit = 2)
        val left = if (splitParts[0].isEmpty()) emptyList() else splitParts[0].split(":")
        val right = if (splitParts.size > 1 && splitParts[1].isNotEmpty()) splitParts[1].split(":") else emptyList()
        val missing = 8 - left.size - right.size
        require(missing >= 0) { "Invalid IPv6 address: $this" }

        val middle = List(missing) { "0000" }
        return (left + middle + right).joinToString(":") { it.padStart(4, '0') }
    }

    return split(":").joinToString(":") { it.padStart(4, '0') }
}

internal sealed class IpRange {
    data class Ipv4(
        val start: Long,
        val end: Long,
    ) : IpRange() {
        init {
            require(start <= end) { "Invalid range: start ($start) > end ($end)" }
            require(start >= 0 && end <= 0xFFFFFFFFL) { "IPv4 range out of bounds" }
        }
    }

    data class Ipv6(
        val startHigh: Long,
        val startLow: Long,
        val endHigh: Long,
        val endLow: Long,
    ) : IpRange() {
        init {
            val start = (startHigh.toULong() shl 64) or startLow.toULong()
            val end = (endHigh.toULong() shl 64) or endLow.toULong()
            require(start <= end) { "Invalid range: start > end" }
        }
    }
}
