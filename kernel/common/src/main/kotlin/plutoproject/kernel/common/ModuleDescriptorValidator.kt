package plutoproject.kernel.common

import plutoproject.kernel.api.MODULE_DESCRIPTOR_SCHEMA_VERSION
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

data class ModuleValidationError(
    val platform: Platform,
    val moduleId: String?,
    val message: String,
)

class ModuleValidationException(
    val errors: List<ModuleValidationError>,
) : IllegalArgumentException(errors.joinToString("\n") { it.message })

object ModuleDescriptorValidator {
    private val idPattern = Regex("[a-z][a-z0-9_-]*")

    fun validate(descriptors: Collection<ModuleDescriptor>): Map<Platform, List<ModuleDescriptor>> {
        val errors = mutableListOf<ModuleValidationError>()
        val byPlatform = descriptors.groupBy(ModuleDescriptor::platform)
        byPlatform.forEach { (platform, platformDescriptors) ->
            validatePlatform(platform, platformDescriptors, errors)
        }
        if (errors.isNotEmpty()) throw ModuleValidationException(errors)
        return byPlatform
    }

    fun validateForPlatform(
        platform: Platform,
        descriptors: Collection<ModuleDescriptor>,
    ): List<ModuleDescriptor> {
        val errors = descriptors.filter { it.platform != platform }.map {
            ModuleValidationError(platform, it.id, "Module '${it.id}' targets ${it.platform}, expected $platform")
        }.toMutableList()
        validatePlatform(platform, descriptors.filter { it.platform == platform }, errors)
        if (errors.isNotEmpty()) throw ModuleValidationException(errors)
        return descriptors.toList()
    }

    private fun validatePlatform(
        platform: Platform,
        descriptors: List<ModuleDescriptor>,
        errors: MutableList<ModuleValidationError>,
    ) {
        descriptors.forEach { descriptor ->
            if (descriptor.schemaVersion != MODULE_DESCRIPTOR_SCHEMA_VERSION) {
                errors.add(platform, descriptor.id, "Module '${descriptor.id}' has unsupported schema ${descriptor.schemaVersion}")
            }
            if (!idPattern.matches(descriptor.id)) {
                errors.add(platform, descriptor.id, "Module ID '${descriptor.id}' is invalid")
            }
            if (descriptor.entrypoint.isBlank()) {
                errors.add(platform, descriptor.id, "Module '${descriptor.id}' has a blank entrypoint")
            }
        }
        descriptors.groupBy(ModuleDescriptor::id).filterValues { it.size > 1 }.forEach { (id) ->
            errors.add(platform, id, "Duplicate module ID '$id' for $platform")
        }
        val modules = descriptors.associateBy(ModuleDescriptor::id)
        descriptors.forEach { descriptor -> validateDependencies(platform, descriptor, modules, errors) }
        detectRequiredCycles(platform, descriptors, modules, errors)
    }

    private fun validateDependencies(
        platform: Platform,
        descriptor: ModuleDescriptor,
        modules: Map<String, ModuleDescriptor>,
        errors: MutableList<ModuleValidationError>,
    ) {
        if (descriptor.type == ModuleType.CAPABILITY &&
            (descriptor.requiredFeatures.isNotEmpty() || descriptor.optionalFeatures.isNotEmpty())
        ) {
            errors.add(platform, descriptor.id, "Capability '${descriptor.id}' must not depend on features")
        }
        descriptor.requiredFeatures.forEach { id ->
            val dependency = modules[id]
            when {
                dependency == null -> errors.add(platform, descriptor.id, "Module '${descriptor.id}' requires missing feature '$id'")
                dependency.type != ModuleType.FEATURE -> errors.add(platform, descriptor.id, "Module '${descriptor.id}' requires '$id' as a feature, but it is a capability")
            }
        }
        descriptor.optionalFeatures.forEach { id ->
            val dependency = modules[id]
            if (dependency != null && dependency.type != ModuleType.FEATURE) {
                errors.add(platform, descriptor.id, "Module '${descriptor.id}' references '$id' as an optional feature, but it is a capability")
            }
        }
        descriptor.requiredCapabilities.forEach { id ->
            val dependency = modules[id]
            when {
                dependency == null -> errors.add(platform, descriptor.id, "Module '${descriptor.id}' requires missing capability '$id'")
                dependency.type != ModuleType.CAPABILITY -> errors.add(platform, descriptor.id, "Module '${descriptor.id}' requires '$id' as a capability, but it is a feature")
            }
        }
    }

    private fun detectRequiredCycles(
        platform: Platform,
        descriptors: List<ModuleDescriptor>,
        modules: Map<String, ModuleDescriptor>,
        errors: MutableList<ModuleValidationError>,
    ) {
        val visiting = linkedSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            if (!visiting.add(id)) {
                val cycle = visiting.dropWhile { it != id } + id
                errors.add(platform, id, "Required dependency cycle: ${cycle.joinToString(" -> ")}")
                return
            }
            val descriptor = modules.getValue(id)
            (descriptor.requiredFeatures + descriptor.requiredCapabilities)
                .filter(modules::containsKey)
                .forEach(::visit)
            visiting.remove(id)
            visited += id
        }
        descriptors.forEach { visit(it.id) }
    }

    private fun MutableList<ModuleValidationError>.add(platform: Platform, id: String?, message: String) {
        add(ModuleValidationError(platform, id, message))
    }
}
