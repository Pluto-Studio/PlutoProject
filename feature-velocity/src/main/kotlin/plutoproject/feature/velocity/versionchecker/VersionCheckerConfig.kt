package plutoproject.feature.velocity.versionchecker

data class VersionCheckerConfig(
    val protocolRange: Pair<Int, Int> = 767 to 767,
    val serverBrand: String? = null,
) {
    val intProtocolRange: IntRange
        get() = protocolRange.first..protocolRange.second
}
