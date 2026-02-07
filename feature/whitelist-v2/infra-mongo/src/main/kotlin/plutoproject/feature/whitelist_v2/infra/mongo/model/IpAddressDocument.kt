package plutoproject.feature.whitelist_v2.infra.mongo.model

import kotlinx.serialization.Serializable

@Serializable
data class IpAddressDocument(
    val ip: String,
    val ipBinary: ByteArray,
    val ipVersion: Int,
    val ipHigh: Long,
    val ipLow: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IpAddressDocument

        if (ipVersion != other.ipVersion) return false
        if (ipHigh != other.ipHigh) return false
        if (ipLow != other.ipLow) return false
        if (ip != other.ip) return false
        if (!ipBinary.contentEquals(other.ipBinary)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipVersion
        result = 31 * result + ipHigh.hashCode()
        result = 31 * result + ipLow.hashCode()
        result = 31 * result + ip.hashCode()
        result = 31 * result + ipBinary.contentHashCode()
        return result
    }
}
