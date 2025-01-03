package plutoproject.framework.common.api.feature

enum class State(val stage: Int) {
    UNINITIALIZED(0), INITIALIZED(1), LOADED(2), ENABLED(3), DISABLED(4)
}
