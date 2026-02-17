package plutoproject.feature.whitelist_v2.infra.mongo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.net.InetAddress

class IpAddressUtilsTest {
    @Test
    fun `Inet4Address toLongs should map to low 32 bits`() {
        val ip = InetAddress.getByName("127.0.0.1")
        val (high, low) = ip.toLongs()

        assertEquals(0L, high)
        assertEquals(0x7F000001L, low)
    }

    @Test
    fun `Inet4Address toLongs should keep unsigned bytes`() {
        val ip = InetAddress.getByName("255.255.255.255")
        val (high, low) = ip.toLongs()

        assertEquals(0L, high)
        assertEquals(0xFFFF_FFFFL, low)
    }

    @Test
    fun `Inet6Address toLongs should equal big endian byte buffer`() {
        val ip = InetAddress.getByName("2001:db8::1")
        val bytes = ip.address

        val buffer = ByteBuffer.wrap(bytes)
        val expectedHigh = buffer.long
        val expectedLow = buffer.long

        val (high, low) = ip.toLongs()
        assertEquals(expectedHigh, high)
        assertEquals(expectedLow, low)
    }

    @Test
    fun `toIpRange should parse ipv4 cidr`() {
        val range = "192.168.1.10/24".toIpRange() as IpRange.Ipv4
        assertEquals(0xC0A8_0100L, range.start)
        assertEquals(0xC0A8_01FFL, range.end)
    }

    @Test
    fun `toIpRange should parse ipv4 prefix 0 as full range`() {
        val range = "10.1.2.3/0".toIpRange() as IpRange.Ipv4
        assertEquals(0L, range.start)
        assertEquals(0xFFFF_FFFFL, range.end)
    }

    @Test
    fun `toIpRange should parse ipv4 prefix 32 as single host`() {
        val range = "8.8.4.4/32".toIpRange() as IpRange.Ipv4
        assertEquals(0x0808_0404L, range.start)
        assertEquals(0x0808_0404L, range.end)
    }

    @Test
    fun `toIpRange should reject invalid ipv4 prefix`() {
        assertThrows(IllegalArgumentException::class.java) {
            "127.0.0.1/33".toIpRange()
        }
    }

    @Test
    fun `toIpRange should reject invalid ipv4 address`() {
        assertThrows(IllegalArgumentException::class.java) {
            "127.0.0/24".toIpRange()
        }
        assertThrows(IllegalArgumentException::class.java) {
            "127.0.0.256/24".toIpRange()
        }
    }

    @Test
    fun `toIpRange should parse ipv6 prefix 0 as full range`() {
        val range = "2001:db8::/0".toIpRange() as IpRange.Ipv6
        assertEquals(0L, range.startHigh)
        assertEquals(0L, range.startLow)
        assertEquals(-1L, range.endHigh)
        assertEquals(-1L, range.endLow)
    }

    @Test
    fun `toIpRange should parse ipv6 prefix 64 with low range`() {
        val ip = InetAddress.getByName("2001:db8::")
        val (expectedHigh, _) = ip.toLongs()

        val range = "2001:db8::/64".toIpRange() as IpRange.Ipv6
        assertEquals(expectedHigh, range.startHigh)
        assertEquals(0L, range.startLow)
        assertEquals(expectedHigh, range.endHigh)
        assertEquals(-1L, range.endLow)
    }

    @Test
    fun `toIpRange should parse ipv6 prefix less than 64`() {
        val ip = InetAddress.getByName("2001:db8::")
        val (high, _) = ip.toLongs()
        val mask = -1L shl 32
        val expectedStartHigh = high and mask
        val expectedEndHigh = expectedStartHigh or mask.inv()

        val range = "2001:db8::/32".toIpRange() as IpRange.Ipv6
        assertEquals(expectedStartHigh, range.startHigh)
        assertEquals(0L, range.startLow)
        assertEquals(expectedEndHigh, range.endHigh)
        assertEquals(-1L, range.endLow)
    }

    @Test
    fun `toIpRange should parse ipv6 prefix greater than 64`() {
        val ip = InetAddress.getByName("2001:db8::1")
        val (expectedHigh, expectedLow) = ip.toLongs()
        val mask = -1L shl 32
        val expectedStartLow = expectedLow and mask
        val expectedEndLow = expectedStartLow or mask.inv()

        val range = "2001:db8::1/96".toIpRange() as IpRange.Ipv6
        assertEquals(expectedHigh, range.startHigh)
        assertEquals(expectedStartLow, range.startLow)
        assertEquals(expectedHigh, range.endHigh)
        assertEquals(expectedEndLow, range.endLow)
    }

    @Test
    fun `toIpRange should parse ipv4 mapped ipv6`() {
        val range = "::ffff:192.0.2.128/128".toIpRange() as IpRange.Ipv6
        assertEquals(0L, range.startHigh)
        assertEquals(0x0000FFFFC0000280L, range.startLow)
        assertEquals(0L, range.endHigh)
        assertEquals(0x0000FFFFC0000280L, range.endLow)
    }

    @Test
    fun `toIpRange should reject invalid cidr format`() {
        assertThrows(IllegalArgumentException::class.java) {
            "127.0.0.1".toIpRange()
        }
        assertThrows(IllegalArgumentException::class.java) {
            "127.0.0.1/24/1".toIpRange()
        }
    }

    @Test
    fun `toIpRange should reject invalid ipv6 address`() {
        assertThrows(IllegalArgumentException::class.java) {
            "2001:db8:1:2:3:4:5/64".toIpRange()
        }
        assertThrows(NumberFormatException::class.java) {
            "2001:db8:zzzz::/64".toIpRange()
        }
    }

    @Test
    fun `IpRange should validate bounds and order`() {
        assertThrows(IllegalArgumentException::class.java) {
            IpRange.Ipv4(start = 2, end = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IpRange.Ipv4(start = -1, end = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IpRange.Ipv4(start = 0, end = 0x1_0000_0000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IpRange.Ipv6(startHigh = 0, startLow = 1, endHigh = 0, endLow = 0)
        }
    }
}
