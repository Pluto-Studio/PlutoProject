package plutoproject.kernel.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType

class ModuleDescriptorValidatorTest {
    @Test
    fun `same ID on separate platforms is accepted`() {
        val validated = ModuleDescriptorValidator.validate(
            listOf(feature("shared", platform = Platform.PAPER), feature("shared", platform = Platform.VELOCITY)),
        )

        assertEquals(1, validated.getValue(Platform.PAPER).size)
        assertEquals(1, validated.getValue(Platform.VELOCITY).size)
    }

    @Test
    fun `duplicate ID within one platform is rejected`() {
        val exception = assertThrows<ModuleValidationException> {
            ModuleDescriptorValidator.validate(listOf(feature("duplicate"), feature("duplicate")))
        }

        assertTrue(exception.errors.any { "Duplicate module ID" in it.message })
    }

    @Test
    fun `missing typed dependency and required cycle are rejected`() {
        val missing = assertThrows<ModuleValidationException> {
            ModuleDescriptorValidator.validate(listOf(feature("a", requiredCapabilities = listOf("missing"))))
        }
        assertTrue(missing.errors.any { "missing capability" in it.message })

        val wrongType = assertThrows<ModuleValidationException> {
            ModuleDescriptorValidator.validate(
                listOf(feature("a", requiredCapabilities = listOf("b")), feature("b")),
            )
        }
        assertTrue(wrongType.errors.any { "but it is a feature" in it.message })

        val cycle = assertThrows<ModuleValidationException> {
            ModuleDescriptorValidator.validate(
                listOf(feature("a", requiredFeatures = listOf("b")), feature("b", requiredFeatures = listOf("a"))),
            )
        }
        assertTrue(cycle.errors.any { "Required dependency cycle" in it.message })

        val illegalCapabilityEdge = assertThrows<ModuleValidationException> {
            ModuleDescriptorValidator.validate(
                listOf(
                    ModuleDescriptor(
                        id = "capability",
                        type = ModuleType.CAPABILITY,
                        platform = Platform.PAPER,
                        entrypoint = "fixture.Capability",
                        requiredFeatures = listOf("feature"),
                    ),
                    feature("feature"),
                ),
            )
        }
        assertTrue(illegalCapabilityEdge.errors.any { "must not depend on features" in it.message })
    }

    @Test
    fun `static validation aborts manager before factory invocation`() {
        var factoryInvocations = 0

        assertThrows<ModuleValidationException> {
            RuntimeModuleManager(
                platform = Platform.PAPER,
                descriptors = listOf(feature("a", requiredFeatures = listOf("missing"))),
                featureRoots = listOf("a"),
                moduleFactory = RuntimeModuleFactory {
                    factoryInvocations++
                    error("must not run")
                },
                contextFactory = ModuleContextFactory { descriptor, koin, services ->
                    TestContext(descriptor.id, koin, services)
                },
            )
        }
        assertEquals(0, factoryInvocations)
    }
}
