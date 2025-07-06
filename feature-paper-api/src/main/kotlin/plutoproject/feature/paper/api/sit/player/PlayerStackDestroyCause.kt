package plutoproject.feature.paper.api.sit.player

enum class PlayerStackDestroyCause(val isCancellable: Boolean) {
    PLUGIN(true),
    ONE_PLAYER_LEFT(false),
    FEATURE_DISABLE(false),
}
