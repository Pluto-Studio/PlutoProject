package plutoproject.kernel.moduleprocessor

internal object ModuleCompilationValidator {
    fun validate(declarations: Collection<ModuleDeclaration>): List<String> = buildList {
        val platforms = declarations.map { it.descriptor.platform }.distinct()
        if (platforms.size > 1) {
            add("A runtime entry project may emit descriptors for only one platform")
        }
        declarations.groupBy { it.descriptor.id }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
            .forEach { add("Duplicate runtime module ID '$it' in one compilation") }
    }
}
