package plutoproject.kernel.common

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.ModuleState

class RuntimeModuleManagerDependencyTest {
    @Test
    fun `features can only be disabled from outermost dependent inward`() = runTest {
        val fixture = ManagerFixture(
            listOf(
                feature("a", requiredFeatures = listOf("b")),
                feature("b", requiredFeatures = listOf("c")),
                feature("c"),
            ),
            listOf("a"),
        )
        fixture.start()

        val rejectedB = fixture.manager.disable("b")
        assertInstanceOf(ModuleOperationResult.Rejected::class.java, rejectedB)
        assertEquals(listOf(listOf("a", "b")), (rejectedB as ModuleOperationResult.Rejected).blockerPaths)
        assertInstanceOf(ModuleOperationResult.Success::class.java, fixture.manager.disable("a"))
        assertInstanceOf(ModuleOperationResult.Rejected::class.java, fixture.manager.disable("c"))
        assertInstanceOf(ModuleOperationResult.Success::class.java, fixture.manager.disable("b"))
        assertInstanceOf(ModuleOperationResult.Success::class.java, fixture.manager.disable("c"))
    }

    @Test
    fun `optional feature in plan becomes active edge`() = runTest {
        val fixture = ManagerFixture(
            listOf(feature("a", optionalFeatures = listOf("b")), feature("b")),
            listOf("a", "b"),
        )

        fixture.start()

        assertEquals(setOf(ModuleEdge("a", "b")), fixture.manager.activeOptionalDependencies())
        assertEquals(listOf("b.load", "a.load", "b.enable", "a.enable"), fixture.events)
        val rejected = fixture.manager.disable("b") as ModuleOperationResult.Rejected
        assertEquals(listOf(listOf("a", "b")), rejected.blockerPaths)
    }

    @Test
    fun `optional feature outside plan remains discovered and uncreated`() = runTest {
        val fixture = ManagerFixture(
            listOf(feature("a", optionalFeatures = listOf("b")), feature("b")),
            listOf("a"),
        )

        fixture.start()

        assertEquals(ModuleState.DISCOVERED, fixture.manager.registry.state("b"))
        assertTrue("b" !in fixture.creations)
        assertTrue(fixture.manager.activeOptionalDependencies().isEmpty())
    }

    @Test
    fun `optional cycle in proposed startup plan is rejected before lifecycle`() {
        assertThrows<ModulePlanException> {
            ManagerFixture(
                listOf(
                    feature("a", optionalFeatures = listOf("b")),
                    feature("b", optionalFeatures = listOf("a")),
                ),
                listOf("a", "b"),
            )
        }
    }

    @Test
    fun `capability cannot be disabled and remains enabled without consumers`() = runTest {
        val fixture = ManagerFixture(
            listOf(feature("consumer", requiredCapabilities = listOf("mongo")), capability("mongo")),
            listOf("consumer"),
        )
        fixture.start()

        val rejected = fixture.manager.disable("mongo")
        assertInstanceOf(ModuleOperationResult.Rejected::class.java, rejected)
        fixture.manager.disable("consumer")
        assertEquals(ModuleState.ENABLED, fixture.manager.registry.state("mongo"))
    }
}
