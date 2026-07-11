package plutoproject.kernel.paper

import java.lang.reflect.Proxy
import java.nio.file.Path
import java.util.logging.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.paper.PaperModuleContext

class PaperKernelTest {
    @Test
    fun `discovers and manages a module through Paper bootstrap`() = runTest {
        PaperTestModule.reset()
        val kernel = PaperKernel(
            plugin = paperPlugin(),
            dataFolder = Path.of("build/test-data/paper-kernel"),
            featureRoots = listOf("paper_test"),
            classLoader = javaClass.classLoader,
        )

        kernel.load()
        kernel.enable()
        val job = PaperTestModule.job
        kernel.shutdown()

        assertEquals(listOf("load", "enable", "disable"), PaperTestModule.events)
        assertFalse(job.isActive)
    }

    private fun paperPlugin(): Plugin = Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(Plugin::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getLogger" -> Logger.getLogger("PaperKernelTest")
            else -> null
        }
    } as Plugin
}

class PaperTestModule : RuntimeModule {
    override suspend fun onLoad(context: ModuleContext) {
        check(context is PaperModuleContext)
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
