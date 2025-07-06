package plutoproject.feature.paper.api.sit.player

enum class PlayerStackQuitCause(val isCancellable: Boolean) {
    PLUGIN(true),
    INITIATIVE(true),
    STACK_DESTROY(false),
}
