package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.ModuleState

data class ModuleInspection(
    val snapshot: ModuleSnapshot,
    val activeOptionalDependencies: List<String>,
    val enabledDirectDependents: List<String>,
)

class ModuleManagementService internal constructor(
    private val manager: RuntimeModuleManager,
) {
    fun snapshots(): List<ModuleSnapshot> = manager.registry.snapshots()

    fun inspect(id: String): ModuleInspection? {
        val snapshot = manager.registry.snapshot(id) ?: return null
        val activeOptional = manager.activeOptionalDependencies()
        val enabledDependents = manager.registry.snapshots()
            .filter { it.state == ModuleState.ENABLED }
            .filter { candidate ->
                id in candidate.descriptor.requiredFeatures ||
                    id in candidate.descriptor.requiredCapabilities ||
                    ModuleEdge(candidate.descriptor.id, id) in activeOptional
            }
            .map { it.descriptor.id }
            .sorted()
        return ModuleInspection(
            snapshot = snapshot,
            activeOptionalDependencies = activeOptional
                .filter { it.dependent == id }
                .map(ModuleEdge::dependency)
                .sorted(),
            enabledDirectDependents = enabledDependents,
        )
    }

    fun dependencyPaths(id: String): List<List<String>> {
        if (manager.registry.descriptor(id) == null) return emptyList()
        val activeOptional = manager.activeOptionalDependencies().groupBy(ModuleEdge::dependent)
        val paths = mutableListOf<List<String>>()
        fun visit(current: String, path: List<String>) {
            val descriptor = manager.registry.descriptor(current) ?: return
            val dependencies = (
                descriptor.requiredCapabilities +
                    descriptor.requiredFeatures +
                    activeOptional[current].orEmpty().map(ModuleEdge::dependency)
                ).distinct()
            if (dependencies.isEmpty()) {
                paths += path
                return
            }
            dependencies.forEach { dependency -> visit(dependency, path + dependency) }
        }
        visit(id, listOf(id))
        return paths
    }

    suspend fun disable(id: String): ModuleOperationResult = manager.disable(id)
}
