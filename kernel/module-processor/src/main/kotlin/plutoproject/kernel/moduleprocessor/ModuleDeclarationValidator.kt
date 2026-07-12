package plutoproject.kernel.moduleprocessor

import plutoproject.kernel.api.ModuleType

internal object ModuleDeclarationValidator {
    private val idPattern = Regex("[a-z][a-z0-9_-]*")

    fun validate(declaration: ModuleDeclaration): List<String> = buildList {
        val descriptor = declaration.descriptor
        if (!idPattern.matches(descriptor.id)) {
            add("Module ID '${descriptor.id}' must match ${idPattern.pattern}")
        }
        if (!declaration.isClass) add("Entrypoint must be a class, not an object or interface")
        if (!declaration.isPublic) add("Entrypoint must be public")
        if (declaration.isAbstract) add("Entrypoint must not be abstract")
        if (declaration.isInner) add("Entrypoint must not be an inner class")
        if (!declaration.implementsRuntimeModule) add("Entrypoint must implement RuntimeModule")
        if (!declaration.hasPublicZeroArgumentConstructor) {
            add("Entrypoint must have a public zero-argument constructor")
        }

        val dependencyGroups = listOf(
            "requiredFeatures" to descriptor.requiredFeatures,
            "optionalFeatures" to descriptor.optionalFeatures,
            "requiredCapabilities" to descriptor.requiredCapabilities,
        )
        dependencyGroups.forEach { (name, ids) ->
            ids.filterNot(idPattern::matches).forEach {
                add("$name contains invalid module ID '$it'")
            }
            ids.groupingBy(String::lowercase).eachCount()
                .filterValues { it > 1 }
                .keys
                .forEach { add("$name contains duplicate module ID '$it'") }
            if (descriptor.id in ids) add("Module '${descriptor.id}' must not depend on itself in $name")
        }

        val featureOverlap = descriptor.requiredFeatures.toSet()
            .intersect(descriptor.optionalFeatures.toSet())
        if (featureOverlap.isNotEmpty()) {
            add("Features cannot be both required and optional: ${featureOverlap.sorted().joinToString()}")
        }
        if (descriptor.type == ModuleType.CAPABILITY &&
            (descriptor.requiredFeatures.isNotEmpty() || descriptor.optionalFeatures.isNotEmpty())
        ) {
            add("Capabilities must not depend on features")
        }
    }
}
