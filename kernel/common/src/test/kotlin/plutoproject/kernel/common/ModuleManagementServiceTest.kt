package plutoproject.kernel.common

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModuleManagementServiceTest {
    @Test
    fun `inspection reports dependencies dependents and graph paths`() = runTest {
        val fixture = ManagerFixture(
            descriptors = listOf(
                capability("storage"),
                feature("base", requiredCapabilities = listOf("storage")),
                feature("consumer", optionalFeatures = listOf("base")),
            ),
            roots = listOf("base", "consumer"),
        )
        fixture.start()
        val management = ModuleManagementService(fixture.manager)

        assertEquals(listOf("consumer"), management.inspect("base")?.enabledDirectDependents)
        assertEquals(listOf("base"), management.inspect("consumer")?.activeOptionalDependencies)
        assertEquals(listOf(listOf("consumer", "base", "storage")), management.dependencyPaths("consumer"))
    }
}
