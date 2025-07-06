package plutoproject.feature.paper.api.sit.player

enum class PlayerStackDestroyCause(val isCancellable: Boolean) {
    PLUGIN(true),
    NO_PLAYER_LEFT(false),
    FEATURE_DISABLE(false),
}
