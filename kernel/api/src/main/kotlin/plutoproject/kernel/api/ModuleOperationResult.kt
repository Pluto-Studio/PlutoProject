package plutoproject.kernel.api

sealed interface ModuleOperationResult {
    val id: String
    val operation: ModuleOperation

    data class Success(
        override val id: String,
        override val operation: ModuleOperation,
        val previousState: ModuleState,
        val state: ModuleState,
    ) : ModuleOperationResult

    data class Rejected(
        override val id: String,
        override val operation: ModuleOperation,
        val reason: String,
        val blockerPaths: List<List<String>> = emptyList(),
    ) : ModuleOperationResult

    data class Failed(
        override val id: String,
        override val operation: ModuleOperation,
        val phase: String,
        val cause: Throwable,
    ) : ModuleOperationResult
}
