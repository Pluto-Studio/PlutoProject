package plutoproject.feature.home.paper

data class HomeConfig(
    val maxHomes: Int = 20,
    val maxHomesGrayTest: MaxHomesGrayTestConfig = MaxHomesGrayTestConfig(),
    val nameLengthLimit: Int = 16,
    val blacklistedWorlds: List<String> = emptyList()
)

data class MaxHomesGrayTestConfig(
    val enabled: Boolean = false,
    val maxHomes: Int = 15,
    val existingPlayersEnabled: Boolean = false,
    val existingPlayersPercentage: Int = 0,
) {
    init {
        require(existingPlayersPercentage in 0..100) {
            "max-homes-gray-test.existing-players-percentage must be between 0 and 100"
        }
    }
}
