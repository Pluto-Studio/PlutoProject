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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.currentModuleContextOrNull
import plutoproject.kernel.api.paper.PaperModuleContext

class PaperKernelTest {
    @Test
    fun `discovers and manages a module through Paper bootstrap`() = runTest {
        PaperTestModule.reset()
        val kernel = PaperKernel(
            plugin = paperPlugin(),
            dataFolder = Path.of("build/test-data/paper-kernel"),
            featureRoots = listOf("paper_test"),
            registerCommands = false,
            classLoader = javaClass.classLoader,
        )

        kernel.load()
        assertSame(PaperTestModule.context, PaperTestModule.instance.contextFromStack())
        kernel.enable()
        val job = PaperTestModule.job
        kernel.shutdown()

        assertEquals(listOf("load", "enable", "disable"), PaperTestModule.events)
        assertFalse(job.isActive)
        assertNull(PaperTestModule.instance.contextFromStackOrNull())
    }

    @Test
    fun `only one kernel owns the process slot at a time`() = runTest {
        val first = paperKernel("first")
        val second = paperKernel("second")

        first.load()
        assertThrows<IllegalStateException> { second.load() }
        first.shutdown()

        second.load()
        second.shutdown()
    }

    private fun paperKernel(name: String) = PaperKernel(
        plugin = paperPlugin(),
        dataFolder = Path.of("build/test-data/paper-kernel-$name"),
        featureRoots = listOf("paper_test"),
        registerCommands = false,
        classLoader = javaClass.classLoader,
    )

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
    init {
        instance = this
        context = currentModuleContext()
    }

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

    fun contextFromStack(): ModuleContext = currentModuleContext()

    fun contextFromStackOrNull(): ModuleContext? = currentModuleContextOrNull()

    companion object {
        val events = mutableListOf<String>()
        lateinit var job: Job
        lateinit var instance: PaperTestModule
        lateinit var context: ModuleContext

        fun reset() = events.clear()
    }
}
