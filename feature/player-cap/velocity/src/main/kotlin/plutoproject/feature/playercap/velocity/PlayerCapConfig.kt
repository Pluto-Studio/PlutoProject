package plutoproject.feature.playercap.velocity

data class PlayerCapConfig(
    val forwardPlayerList: Boolean = true,
    val samplePlayersCount: Int = 20,
    val maxPlayerCount: Int = 100,
)
