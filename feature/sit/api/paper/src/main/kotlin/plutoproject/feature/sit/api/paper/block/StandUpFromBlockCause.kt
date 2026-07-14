package plutoproject.feature.sit.api.paper.block

enum class StandUpFromBlockCause(val isCancellable: Boolean) {
    PLUGIN(true),
    INITIATIVE(true),
    QUIT(false),
    DAMAGE(true),
    DEATH(true),
    TELEPORT(true),
    SEAT_BREAK(true),
    SEAT_ENTITY_REMOVE(false),
    FEATURE_DISABLE(false)
}
