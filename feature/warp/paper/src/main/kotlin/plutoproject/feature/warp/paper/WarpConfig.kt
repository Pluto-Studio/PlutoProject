package plutoproject.feature.warp.paper

data class WarpConfig(
    val nameLengthLimit: Int = 32,
    val blacklistedWorlds: List<String> = emptyList(),
)
