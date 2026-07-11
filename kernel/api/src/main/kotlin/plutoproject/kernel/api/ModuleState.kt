package plutoproject.kernel.api

enum class ModuleState {
    DISCOVERED,
    LOADED,
    ENABLED,
    DISABLED,
    BLOCKED,
    FAILED,
}

enum class ModuleOperation {
    LOAD,
    ENABLE,
    DISABLE,
}
