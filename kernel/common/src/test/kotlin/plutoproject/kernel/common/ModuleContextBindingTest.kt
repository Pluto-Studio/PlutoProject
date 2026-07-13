package plutoproject.kernel.common

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import plutoproject.kernel.api.InternalKernelApi
import plutoproject.kernel.api.ModuleContextBinding
import plutoproject.kernel.api.currentModuleContextOrNull
import plutoproject.kernel.common.callback.ClosedContextCallback

@OptIn(InternalKernelApi::class)
class ModuleContextBindingTest {
    @Test
    fun `ordinary classes resolve their indexed module context`() {
        val koin = koinApplication()
        val services = RuntimeServiceRegistry().owner("ordinary", koin.koin)
        val context = TestContext("ordinary", koin.koin, services)
        try {
            ModuleContextBinding.configure(mapOf("plutoproject.kernel.common" to "ordinary"))
            ModuleContextBinding.register(context, "ordinary")

            assertSame(context, currentModuleContextOrNull())
        } finally {
            ModuleContextBinding.close("ordinary")
            context.coroutineScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            services.close()
            koin.close()
        }
    }

    @Test
    fun `a closed indexed module does not fall through to an outer frame`() {
        val koin = koinApplication()
        val services = RuntimeServiceRegistry().owner("closed", koin.koin)
        val context = TestContext("closed", koin.koin, services)
        try {
            ModuleContextBinding.configure(
                mapOf(
                    "plutoproject.kernel.common.callback" to "closed",
                    "plutoproject.kernel.common" to "outer",
                ),
            )
            ModuleContextBinding.register(context, "closed")
            ModuleContextBinding.close("closed")

            assertNull(ClosedContextCallback.lookup())
        } finally {
            context.coroutineScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            services.close()
            koin.close()
        }
    }
}
