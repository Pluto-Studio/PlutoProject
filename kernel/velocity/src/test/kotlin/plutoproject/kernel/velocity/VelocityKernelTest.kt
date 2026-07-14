package plutoproject.kernel.velocity

import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import java.lang.reflect.Proxy
import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.velocity.VelocityModuleContext

class VelocityKernelTest {
    @Test
    fun `discovers and manages a module through Velocity bootstrap`() = runTest {
        VelocityTestModule.reset()
        val kernel = VelocityKernel(
            proxyServer = proxy(ProxyServer::class.java),
            pluginContainer = proxy(PluginContainer::class.java),
            logger = Logger.getLogger("VelocityKernelTest"),
            dataFolder = Path.of("build/test-data/velocity-kernel"),
            featureRoots = listOf("velocity_test"),
            classLoader = javaClass.classLoader,
        )

        kernel.load()
        kernel.enable()
        val job = VelocityTestModule.job
        kernel.shutdown()

        assertEquals(listOf("load", "enable", "disable"), VelocityTestModule.events)
        assertFalse(job.isActive)
    }

    private fun <T> proxy(type: Class<T>): T = type.cast(
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(type)) { _, _, _ -> null },
    )
}

class VelocityTestModule : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        check(context is VelocityModuleContext)
        events += "load"
    }

    override suspend fun onEnable(context: ModuleContext) {
        events += "enable"
        job = context.coroutineScope.launch { awaitCancellation() }
    }

    override suspend fun onDisable(context: ModuleContext) {
        events += "disable"
    }

    companion object {
        val events = mutableListOf<String>()
        lateinit var job: Job

        fun reset() = events.clear()
    }
}
