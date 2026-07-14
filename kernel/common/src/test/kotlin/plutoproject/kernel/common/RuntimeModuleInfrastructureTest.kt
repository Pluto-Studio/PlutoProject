package plutoproject.kernel.common

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.exportService
import plutoproject.kernel.api.koinGet
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.loadKoinModuleDefinitions

class RuntimeModuleInfrastructureTest {
    @Test
    fun `module containers are isolated and close after shutdown`() = runTest {
        val closed = mutableMapOf<String, AtomicBoolean>()
        val values = mutableMapOf<String, String>()
        val parameterized = mutableMapOf<String, String>()
        val eager = mutableMapOf<String, AtomicBoolean>()
        val fixture = infrastructureFixture(listOf(feature("a"), feature("b")), listOf("a", "b")) { id ->
            object : RuntimeModule {
                override suspend fun onLoad(context: ModuleContext) {
                    val marker = AtomicBoolean()
                    closed[id] = marker
                    val eagerCreated = AtomicBoolean()
                    eager[id] = eagerCreated
                    context.loadKoinModuleDefinitions(module {
                        single { "$id-value" }
                        single { CloseMarker(marker) }.onClose { it?.close() }
                        factory(named("parameterized")) { parameters -> "${parameters.get<String>()}-$id" }
                    })
                    context.loadKoinModuleDefinitions(module(createdAtStart = true) {
                        single(named("eager")) { eagerCreated.apply { set(true) } }
                    })
                    context.koinGet<CloseMarker>()
                    values[id] = context.koinGet()
                    parameterized[id] = context.koinGet(named("parameterized")) { parametersOf("input") }
                }
            }
        }

        fixture.manager.loadStartup()
        fixture.manager.enableStartup()

        assertEquals(mapOf("a" to "a-value", "b" to "b-value"), values)
        assertEquals(mapOf("a" to "input-a", "b" to "input-b"), parameterized)
        assertTrue(eager.values.all(AtomicBoolean::get))
        assertFalse(closed.values.any(AtomicBoolean::get))

        fixture.manager.shutdown()

        assertTrue(closed.values.all(AtomicBoolean::get))
    }

    @Test
    fun `services support export query unregister and lifecycle states`() {
        val registry = RuntimeServiceRegistry()
        val providerKoin = koinApplication()
        val consumerKoin = koinApplication()
        val provider = registry.owner("provider", providerKoin.koin)
        val consumer = registry.owner("consumer", consumerKoin.koin)

        assertThrows<IllegalStateException> { provider.exportService<String>("early") }
        assertNull(consumer.getServiceOrNull(String::class))
        provider.activate()
        consumer.activate()

        val first = provider.exportService<String>("first")
        assertEquals("first", consumer.getService<String>())
        assertThrows<IllegalStateException> { provider.exportService<String>("duplicate") }
        first.unregister()
        assertFalse(first.isRegistered)
        val second = provider.exportService<String>("second")
        first.unregister()
        assertTrue(second.isRegistered)
        assertEquals("second", consumer.getService<String>())

        provider.beginClosing()
        assertFalse(second.isRegistered)
        assertThrows<IllegalStateException> { provider.exportService<String>("late") }
        provider.close()
        assertThrows<IllegalStateException> { provider.getServiceOrNull(String::class) }
        consumer.close()
        providerKoin.close()
        consumerKoin.close()
    }

    @Test
    fun `export racing owner close cannot leak a service`() {
        val registry = RuntimeServiceRegistry()
        val providerKoin = koinApplication()
        val consumerKoin = koinApplication()
        val provider = registry.owner("provider", providerKoin.koin).also { it.activate() }
        val consumer = registry.owner("consumer", consumerKoin.koin).also { it.activate() }
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val export = executor.submit {
                start.await()
                runCatching { provider.exportService<String>("value") }
            }
            val close = executor.submit {
                start.await()
                provider.beginClosing()
            }
            start.countDown()
            export.get()
            close.get()
            assertNull(consumer.getServiceOrNull(String::class))
        } finally {
            executor.shutdownNow()
            provider.close()
            consumer.close()
            providerKoin.close()
            consumerKoin.close()
        }
    }
}

private class CloseMarker(private val closed: AtomicBoolean) : AutoCloseable {
    override fun close() {
        closed.set(true)
    }
}

private data class InfrastructureFixture(
    val manager: RuntimeModuleManager,
    val contexts: Map<String, TestContext>,
)

private fun infrastructureFixture(
    descriptors: List<plutoproject.kernel.api.ModuleDescriptor>,
    roots: List<String>,
    module: (String) -> RuntimeModule,
): InfrastructureFixture {
    val contexts = mutableMapOf<String, TestContext>()
    val manager = RuntimeModuleManager(
        platform = plutoproject.kernel.api.Platform.PAPER,
        descriptors = descriptors,
        featureRoots = roots,
        moduleFactory = RuntimeModuleFactory { module(it.id) },
        contextFactory = ModuleContextFactory { descriptor, koin, services ->
            TestContext(descriptor.id, koin, services).also { contexts[descriptor.id] = it }
        },
    )
    return InfrastructureFixture(manager, contexts)
}
