package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType

data class ActivationPlan(
    val capabilities: List<ModuleDescriptor>,
    val features: List<ModuleDescriptor>,
    val proposedOptionalEdges: Set<ModuleEdge>,
    val warnings: List<String>,
) {
    val orderedModules: List<ModuleDescriptor> = capabilities + features
    val ids: Set<String> = orderedModules.mapTo(linkedSetOf(), ModuleDescriptor::id)
}

class ModulePlanException(message: String) : IllegalArgumentException(message)

class ModulePlanner(private val graph: ModuleGraph) {
    fun plan(featureRoots: Collection<String>): ActivationPlan {
        val warnings = mutableListOf<String>()
        val validRoots = featureRoots.distinct().mapNotNull { id ->
            val descriptor = graph.descriptors[id]
            when {
                descriptor == null -> {
                    warnings += "Unknown feature root '$id'"
                    null
                }
                descriptor.type != ModuleType.FEATURE -> {
                    warnings += "Capability '$id' cannot be used as a feature root"
                    null
                }
                else -> id
            }
        }
        val closure = graph.requiredClosure(validRoots)
        val optionalEdges = graph.proposedOptionalEdges(closure)
        graph.validatePlanCycles(closure, optionalEdges)
        val ordered = graph.topologicalOrder(closure, optionalEdges)
        val capabilities = ordered.filter { it.type == ModuleType.CAPABILITY }
        val features = ordered.filter { it.type == ModuleType.FEATURE }
        return ActivationPlan(capabilities, features, optionalEdges, warnings)
    }
}
