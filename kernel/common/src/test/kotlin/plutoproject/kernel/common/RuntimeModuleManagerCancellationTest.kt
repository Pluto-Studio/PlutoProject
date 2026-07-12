package plutoproject.kernel.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleState
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule

class RuntimeModuleManagerCancellationTest {
    @Test
    fun `load cancellation terminates manager and cleans created modules in reverse order`() = runTest {
        val expectedCancellation = CancellationException("load cancellation")
        val events = mutableListOf<String>()
        val fixture = cancellationFixture(
            descriptors = listOf(
                feature("a", requiredFeatures = listOf("b")),
                feature("b", requiredFeatures = listOf("c")),
                feature("c"),
            ),
            roots = listOf("a"),
            modules = mapOf(
                "a" to HookModule(),
                "b" to HookModule(
                    onLoad = {
                        events += "b.load"
                        throw expectedCancellation
                    },
                    onDisable = { events += "b.disable" },
                ),
                "c" to HookModule(
                    onLoad = { events += "c.load" },
                    onDisable = {
                        events += "c.disable"
                        error("c cleanup failure")
                    },
                ),
            ),
        )

        val cancellation = assertThrows<CancellationException> { fixture.manager.loadStartup() }

        assertEquals(listOf("c.load", "b.load", "c.disable"), events)
        assertTrue(cancellation === expectedCancellation)
        assertTrue(cancellation.suppressed.any { it.message == "c cleanup failure" })
        assertTerminated(fixture, "b", "c")
        assertEquals(ModuleState.DISCOVERED, fixture.manager.registry.state("a"))
        assertNull(fixture.manager.registry.snapshot("a")?.runningOperation)
        assertFalse("a" in fixture.contexts)
        assertThrows<IllegalStateException> { fixture.manager.enableStartup() }
    }

    @Test
    fun `enable cancellation disables current and previously enabled modules`() = runTest {
        val enteredEnable = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val fixture = cancellationFixture(
            descriptors = listOf(feature("a", requiredFeatures = listOf("b")), feature("b")),
            roots = listOf("a"),
            modules = mapOf(
                "a" to HookModule(
                    onEnable = {
                        events += "a.enable"
                        enteredEnable.complete(Unit)
                        awaitCancellation()
                    },
                    onDisable = { events += "a.disable" },
                ),
                "b" to HookModule(
                    onEnable = { events += "b.enable" },
                    onDisable = { events += "b.disable" },
                ),
            ),
        )
        fixture.manager.loadStartup()
        val operation = async { fixture.manager.enableStartup() }

        enteredEnable.await()
        operation.cancel()
        assertThrows<CancellationException> { operation.await() }

        assertEquals(listOf("b.enable", "a.enable", "a.disable", "b.disable"), events)
        assertTerminated(fixture, "a", "b")
    }

    @Test
    fun `disable cancellation completes cleanup before propagating`() = runTest {
        val fixture = cancellationFixture(
            descriptors = listOf(feature("single")),
            roots = listOf("single"),
            modules = mapOf(
                "single" to HookModule(onDisable = { throw CancellationException("module cancellation") }),
            ),
        )
        fixture.manager.loadStartup()
        fixture.manager.enableStartup()

        val cause = assertThrows<CancellationException> { fixture.manager.disable("single") }

        assertEquals("module cancellation", cause.message)
        assertTerminated(fixture, "single")
    }

    @Test
    fun `shutdown finishes cleanup after caller cancellation`() = runTest {
        val enteredDisable = CompletableDeferred<Unit>()
        val releaseDisable = CompletableDeferred<Unit>()
        val fixture = cancellationFixture(
            descriptors = listOf(feature("single")),
            roots = listOf("single"),
            modules = mapOf(
                "single" to HookModule(
                    onDisable = {
                        enteredDisable.complete(Unit)
                        releaseDisable.await()
                    },
                ),
            ),
        )
        fixture.manager.loadStartup()
        fixture.manager.enableStartup()
        val operation = launch { fixture.manager.shutdown() }

        enteredDisable.await()
        operation.cancel()
        releaseDisable.complete(Unit)
        operation.join()

        assertTrue(operation.isCancelled)
        assertTerminated(fixture, "single")
    }

    private fun assertTerminated(fixture: CancellationFixture, vararg ids: String) {
        ids.forEach { id ->
            assertEquals(ModuleState.DISABLED, fixture.manager.registry.state(id))
            assertNull(fixture.manager.registry.snapshot(id)?.runningOperation)
            assertFalse(fixture.contexts.getValue(id).job.isActive)
        }
    }
}

private class HookModule(
    private val onLoad: suspend (ModuleContext) -> Unit = {},
    private val onEnable: suspend (ModuleContext) -> Unit = {},
    private val onDisable: suspend (ModuleContext) -> Unit = {},
) : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) = onLoad.invoke(context)

    override suspend fun onEnable(context: ModuleContext) = onEnable.invoke(context)

    override suspend fun onDisable(context: ModuleContext) = onDisable.invoke(context)
}

private data class CancellationFixture(
    val manager: RuntimeModuleManager,
    val contexts: Map<String, TestContext>,
)

private fun cancellationFixture(
    descriptors: List<ModuleDescriptor>,
    roots: List<String>,
    modules: Map<String, RuntimeModule>,
): CancellationFixture {
    val contexts = mutableMapOf<String, TestContext>()
    val manager = RuntimeModuleManager(
        platform = Platform.PAPER,
        descriptors = descriptors,
        featureRoots = roots,
        moduleFactory = RuntimeModuleFactory { modules.getValue(it.id) },
        contextFactory = ModuleContextFactory { descriptor, koin, services ->
            TestContext(descriptor.id, koin, services).also { contexts[descriptor.id] = it }
        },
    )
    return CancellationFixture(manager, contexts)
}
