package plutoproject.kernel.api

interface FeatureRegistry {
    fun state(id: String): ModuleState?

    fun isEnabled(id: String): Boolean
}

interface FeatureController {
    suspend fun disable(id: String): ModuleOperationResult
}
