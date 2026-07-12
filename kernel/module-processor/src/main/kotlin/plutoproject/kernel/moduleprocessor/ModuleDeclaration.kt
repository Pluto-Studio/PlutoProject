package plutoproject.kernel.moduleprocessor

import plutoproject.kernel.api.ModuleDescriptor

internal data class ModuleDeclaration(
    val descriptor: ModuleDescriptor,
    val isClass: Boolean = true,
    val isPublic: Boolean = true,
    val isAbstract: Boolean = false,
    val isInner: Boolean = false,
    val implementsRuntimeModule: Boolean = true,
    val hasPublicZeroArgumentConstructor: Boolean = true,
)
