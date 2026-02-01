package plutoproject.feature.velocity.whitelist_v2

data class WhitelistConfig(
    val enableMigrator: Boolean = false,
    val whitelistApplicationGuide: String = "https://wiki.pmc.ink/",
    val visitorMode: VisitorModeConfig = VisitorModeConfig()
)

data class VisitorModeConfig(
    val enable: Boolean = true,
    val visitorPermissionGroup: String = "visitor"
)
