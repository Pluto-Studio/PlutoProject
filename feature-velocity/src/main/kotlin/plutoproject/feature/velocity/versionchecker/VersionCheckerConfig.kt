package plutoproject.feature.velocity.versionchecker

import com.velocitypowered.api.network.ProtocolVersion

data class VersionCheckerConfig(
    val serverBrand: String = "PlutoProject",
    val supportedProtocolRange: IntRange = MINIMUM_SUPPORTED_VERSION..MAXIMUM_SUPPORTED_VERSION,
    val compatibleProtocolRange: IntRange = supportedProtocolRange,
) {
    init {
        check(supportedProtocolRange.all { it in compatibleProtocolRange }) {
            "Supported protocols must be included in compatible protocols."
        }
    }

    private fun getGameVersionsByProtocol(protocol: Int): List<String> {
        return ProtocolVersion.getProtocolVersion(protocol).versionsSupportedBy
    }

    val minimumSupportedProtocol get() = supportedProtocolRange.first
    val supportedGameVersions get() = supportedProtocolRange.flatMap { getGameVersionsByProtocol(it) }
}
