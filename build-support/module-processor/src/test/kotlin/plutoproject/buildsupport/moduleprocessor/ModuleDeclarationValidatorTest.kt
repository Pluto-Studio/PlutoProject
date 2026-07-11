package plutoproject.buildsupport.moduleprocessor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

class ModuleDeclarationValidatorTest {
    @Test
    fun `accepts valid feature declaration`() {
        val declaration = ModuleDeclaration(
            ModuleDescriptor(
                id = "home",
                type = ModuleType.FEATURE,
                platform = Platform.PAPER,
                entrypoint = "example.HomeFeature",
                requiredFeatures = listOf("teleport"),
                optionalFeatures = listOf("menu"),
                requiredCapabilities = listOf("mongo", "interactive"),
            ),
        )

        assertEquals(emptyList<String>(), ModuleDeclarationValidator.validate(declaration))
    }

    @Test
    fun `rejects invalid entrypoint shape and metadata`() {
        val declaration = ModuleDeclaration(
            descriptor = ModuleDescriptor(
                id = "Invalid ID",
                type = ModuleType.CAPABILITY,
                platform = Platform.VELOCITY,
                entrypoint = "example.InvalidCapability",
                requiredFeatures = listOf("feature"),
                requiredCapabilities = listOf("Invalid ID"),
            ),
            isPublic = false,
            isAbstract = true,
            implementsRuntimeModule = false,
            hasPublicZeroArgumentConstructor = false,
        )

        val errors = ModuleDeclarationValidator.validate(declaration)

        assertTrue(errors.any { "must match" in it })
        assertTrue(errors.any { "must be public" in it })
        assertTrue(errors.any { "must not be abstract" in it })
        assertTrue(errors.any { "implement RuntimeModule" in it })
        assertTrue(errors.any { "zero-argument constructor" in it })
        assertTrue(errors.any { "must not depend on features" in it })
    }

    @Test
    fun `rejects mixed platforms and duplicate IDs in one compilation`() {
        val paper = validDeclaration("shared", Platform.PAPER)
        val velocity = validDeclaration("shared", Platform.VELOCITY)

        val errors = ModuleCompilationValidator.validate(listOf(paper, velocity))

        assertTrue(errors.any { "only one platform" in it })
        assertTrue(errors.any { "Duplicate runtime module ID 'shared'" in it })
    }

    private fun validDeclaration(id: String, platform: Platform) = ModuleDeclaration(
        ModuleDescriptor(
            id = id,
            type = ModuleType.FEATURE,
            platform = platform,
            entrypoint = "example.$id.Entrypoint",
        ),
    )
}
