package plutoproject.feature.paper.api.sit.player

enum class PlayerStackDestroyCause(val isCancellable: Boolean) {
    PLUGIN(true),
    RIGHT_CLICK_SIT_CANCELLED(true),
    ALL_PASSENGER_LEFT(true),
    NO_PLAYER_LEFT(false),
    FEATURE_DISABLE(false),
}
