package plutoproject.feature.paper.api.sit.player

enum class PlayerStackQuitCause(val isCancellable: Boolean) {
    PLUGIN(true),
    INITIATIVE(true),
    QUIT(false),
    DEATH(false),
    SEAT_ENTITY_REMOVE(false),
    STACK_DESTROY(false),
}
