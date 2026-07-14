package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType

data class ModuleEdge(val dependent: String, val dependency: String)

class ModuleGraph(descriptors: Collection<ModuleDescriptor>) {
    val descriptors: Map<String, ModuleDescriptor> = descriptors.associateBy(ModuleDescriptor::id)

    fun requiredDependencies(id: String): List<String> = descriptors.getValue(id).let {
        it.requiredCapabilities + it.requiredFeatures
    }

    fun requiredClosure(roots: Collection<String>): Set<String> {
        val closure = linkedSetOf<String>()
        fun include(id: String) {
            if (!closure.add(id)) return
            requiredDependencies(id).forEach(::include)
        }
        roots.forEach(::include)
        return closure
    }

    fun topologicalOrder(
        ids: Collection<String>,
        additionalEdges: Set<ModuleEdge> = emptySet(),
    ): List<ModuleDescriptor> {
        val included = ids.toSet()
        val additionalByDependent = additionalEdges.groupBy(ModuleEdge::dependent)
        val visited = mutableSetOf<String>()
        val result = mutableListOf<ModuleDescriptor>()
        fun visit(id: String) {
            if (!visited.add(id)) return
            val required = requiredDependencies(id).filter { it in included }
            val additional = additionalByDependent[id].orEmpty().map(ModuleEdge::dependency)
            (required + additional).forEach(::visit)
            result += descriptors.getValue(id)
        }
        ids.sorted().forEach(::visit)
        return result
    }

    fun proposedOptionalEdges(planIds: Set<String>): Set<ModuleEdge> = buildSet {
        planIds.forEach { id ->
            descriptors.getValue(id).optionalFeatures
                .filter { it in planIds }
                .forEach { add(ModuleEdge(id, it)) }
        }
    }

    fun validatePlanCycles(planIds: Set<String>, optionalEdges: Set<ModuleEdge>) {
        val optionalByDependent = optionalEdges.groupBy(ModuleEdge::dependent)
        val visiting = linkedSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            if (!visiting.add(id)) {
                val cycle = visiting.dropWhile { it != id } + id
                throw ModulePlanException("Active dependency cycle: ${cycle.joinToString(" -> ")}")
            }
            val requiredFeatures = descriptors.getValue(id).requiredFeatures.filter { it in planIds }
            val optionalFeatures = optionalByDependent[id].orEmpty().map(ModuleEdge::dependency)
            (requiredFeatures + optionalFeatures).forEach(::visit)
            visiting.remove(id)
            visited += id
        }
        planIds.filter { descriptors.getValue(it).type == ModuleType.FEATURE }.forEach(::visit)
    }

    fun blockerPaths(
        target: String,
        enabledFeatures: Set<String>,
        activeOptionalEdges: Set<ModuleEdge>,
    ): List<List<String>> {
        val edges = buildSet {
            enabledFeatures.forEach { dependent ->
                descriptors.getValue(dependent).requiredFeatures
                    .filter { it in enabledFeatures }
                    .forEach { add(ModuleEdge(dependent, it)) }
            }
            addAll(activeOptionalEdges.filter { it.dependent in enabledFeatures && it.dependency in enabledFeatures })
        }
        val dependenciesByDependent = edges.groupBy(ModuleEdge::dependent)
        val dependents = edges.map(ModuleEdge::dependent).toSet()
        val dependencies = edges.map(ModuleEdge::dependency).toSet()
        val roots = (dependents - dependencies).ifEmpty { dependents }.sorted()
        val paths = mutableListOf<List<String>>()
        fun walk(current: String, path: List<String>) {
            if (current == target) {
                if (path.size > 1) paths += path
                return
            }
            dependenciesByDependent[current].orEmpty().forEach { edge ->
                if (edge.dependency !in path) walk(edge.dependency, path + edge.dependency)
            }
        }
        roots.forEach { walk(it, listOf(it)) }
        if (paths.isEmpty()) {
            edges.filter { it.dependency == target }.forEach { paths += listOf(it.dependent, target) }
        }
        return paths.distinct()
    }
}
