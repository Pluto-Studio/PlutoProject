package plutoproject.kernel.api

import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

@RequiresOptIn(
    message = "This API is reserved for Runtime Kernel implementations.",
    level = RequiresOptIn.Level.ERROR,
)
annotation class InternalKernelApi

private sealed interface ModuleOwner {
    data class Active(val context: ModuleContext) : ModuleOwner
    data object Closed : ModuleOwner
}

private object ModuleContextStorage {
    val current = ThreadLocal<ModuleContext?>()
    @Volatile
    var packageOwners: Map<String, String> = emptyMap()
    val modules = ConcurrentHashMap<String, ModuleOwner>()
}

fun currentModuleContextOrNull(): ModuleContext? {
    ModuleContextStorage.current.get()?.let { return it }
    val moduleId = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk { frames ->
        frames.map { it.declaringClass.packageName }
            .map { ModuleContextStorage.packageOwners[it] }
            .filter { it != null }
            .findFirst()
            .orElse(null)
    }
    return moduleId?.let { (ModuleContextStorage.modules[it] as? ModuleOwner.Active)?.context }
}

fun currentModuleContext(): ModuleContext = currentModuleContextOrNull()
    ?: error("No active Runtime Module context is available on the current call path")

@OptIn(InternalKernelApi::class)
class ModuleContextElement(
    private val context: ModuleContext,
) : ThreadContextElement<ModuleContext?>,
    AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ModuleContextElement>

    override fun updateThreadContext(context: CoroutineContext): ModuleContext? =
        ModuleContextBinding.replace(this.context)

    override fun restoreThreadContext(context: CoroutineContext, oldState: ModuleContext?) {
        ModuleContextBinding.replace(oldState)
    }
}

@InternalKernelApi
object ModuleContextBinding {
    fun configure(packageOwners: Map<String, String>) {
        ModuleContextStorage.packageOwners = packageOwners.toMap()
        ModuleContextStorage.modules.clear()
    }

    fun register(context: ModuleContext, moduleId: String) {
        ModuleContextStorage.modules[moduleId] = ModuleOwner.Active(context)
    }

    fun close(moduleId: String) {
        ModuleContextStorage.modules[moduleId] = ModuleOwner.Closed
    }

    fun <T> withContext(context: ModuleContext, block: () -> T): T {
        val previous = replace(context)
        return try {
            block()
        } finally {
            replace(previous)
        }
    }

    internal fun replace(context: ModuleContext?): ModuleContext? {
        val previous = ModuleContextStorage.current.get()
        if (context == null) ModuleContextStorage.current.remove() else ModuleContextStorage.current.set(context)
        return previous
    }
}
