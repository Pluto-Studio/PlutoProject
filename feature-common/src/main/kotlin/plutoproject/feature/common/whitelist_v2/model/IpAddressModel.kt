package plutoproject.feature.common.whitelist_v2.model

import kotlinx.serialization.Serializable

@Serializable
data class IpAddressModel(
    val ip: String,
    val ipBinary: ByteArray,
    val ipVersion: Int,
    val ipSegments: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IpAddressModel

        if (ipVersion != other.ipVersion) return false
        if (ip != other.ip) return false
        if (!ipBinary.contentEquals(other.ipBinary)) return false
        if (!ipSegments.contentEquals(other.ipSegments)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipVersion
        result = 31 * result + ip.hashCode()
        result = 31 * result + ipBinary.contentHashCode()
        result = 31 * result + ipSegments.contentHashCode()
        return result
    }
}
