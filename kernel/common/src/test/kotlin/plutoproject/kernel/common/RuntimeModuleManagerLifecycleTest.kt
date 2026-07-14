package plutoproject.kernel.common

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.kernel.api.ModuleOperationResult
import plutoproject.kernel.api.ModuleState

class RuntimeModuleManagerLifecycleTest {
    @Test
    fun `required chain loads and enables dependencies first`() = runTest {
        val fixture = ManagerFixture(
            descriptors = listOf(
                feature("a", requiredFeatures = listOf("b")),
                feature("b", requiredFeatures = listOf("c")),
                feature("c"),
            ),
            roots = listOf("a"),
        )

        fixture.start()

        assertEquals(
            listOf("c.load", "b.load", "a.load", "c.enable", "b.enable", "a.enable"),
            fixture.events,
        )
    }

    @Test
    fun `preloaded dependent is cleaned and blocked after dependency enable failure`() = runTest {
        val fixture = ManagerFixture(
            descriptors = listOf(feature("a", requiredFeatures = listOf("b")), feature("b")),
            roots = listOf("a"),
            failures = mapOf("b" to "enable"),
        )

        fixture.start()

        assertEquals(ModuleState.FAILED, fixture.manager.registry.state("b"))
        assertEquals(ModuleState.BLOCKED, fixture.manager.registry.state("a"))
        assertTrue("a.disable" in fixture.events)
        assertFalse(fixture.contexts.getValue("a").job.isActive)
        assertEquals(listOf("a", "b"), fixture.manager.registry.snapshot("a")?.dependencyPath)
    }

    @Test
    fun `dependent failure leaves successfully started dependency enabled`() = runTest {
        val fixture = ManagerFixture(
            descriptors = listOf(feature("a", requiredFeatures = listOf("b")), feature("b")),
            roots = listOf("a"),
            failures = mapOf("a" to "enable"),
        )

        fixture.start()

        assertEquals(ModuleState.FAILED, fixture.manager.registry.state("a"))
        assertEquals(ModuleState.ENABLED, fixture.manager.registry.state("b"))
        val failure = fixture.manager.registry.snapshot("a")?.latestResult as ModuleOperationResult.Failed
        assertEquals("onEnable", failure.phase)
        assertEquals("a enable failure", failure.cause.message)
    }

    @Test
    fun `independent module continues after lifecycle failure`() = runTest {
        val fixture = ManagerFixture(
            descriptors = listOf(feature("broken"), feature("healthy")),
            roots = listOf("broken", "healthy"),
            failures = mapOf("broken" to "load"),
        )

        fixture.start()

        assertEquals(ModuleState.FAILED, fixture.manager.registry.state("broken"))
        assertEquals(ModuleState.ENABLED, fixture.manager.registry.state("healthy"))
        assertTrue("healthy.enable" in fixture.events)
    }

    @Test
    fun `failed capability and disabled feature cannot be retried`() = runTest {
        val capabilityFixture = ManagerFixture(
            descriptors = listOf(feature("consumer", requiredCapabilities = listOf("mongo")), capability("mongo")),
            roots = listOf("consumer"),
            failures = mapOf("mongo" to "load"),
        )
        capabilityFixture.start()
        capabilityFixture.manager.loadStartup()
        capabilityFixture.manager.enableStartup()
        assertEquals(1, capabilityFixture.creations["mongo"])
        assertEquals(ModuleState.FAILED, capabilityFixture.manager.registry.state("mongo"))

        val featureFixture = ManagerFixture(listOf(feature("single")), listOf("single"))
        featureFixture.start()
        featureFixture.manager.disable("single")
        featureFixture.manager.loadStartup()
        featureFixture.manager.enableStartup()
        assertEquals(1, featureFixture.creations["single"])
        assertEquals(ModuleState.DISABLED, featureFixture.manager.registry.state("single"))
    }

    @Test
    fun `disable failure still releases scope and permanently disables feature`() = runTest {
        val fixture = ManagerFixture(
            listOf(feature("broken-disable")),
            listOf("broken-disable"),
            failures = mapOf("broken-disable" to "disable"),
        )
        fixture.start()

        val result = fixture.manager.disable("broken-disable")

        assertInstanceOf(ModuleOperationResult.Failed::class.java, result)
        assertEquals(ModuleState.DISABLED, fixture.manager.registry.state("broken-disable"))
        assertFalse(fixture.contexts.getValue("broken-disable").job.isActive)
    }

    @Test
    fun `shutdown disables features before capabilities`() = runTest {
        val fixture = ManagerFixture(
            listOf(feature("consumer", requiredCapabilities = listOf("mongo")), capability("mongo")),
            listOf("consumer"),
        )
        fixture.start()

        fixture.manager.shutdown()

        assertEquals(listOf("consumer.disable", "mongo.disable"), fixture.events.takeLast(2))
        assertEquals(ModuleState.DISABLED, fixture.manager.registry.state("mongo"))
    }
}
