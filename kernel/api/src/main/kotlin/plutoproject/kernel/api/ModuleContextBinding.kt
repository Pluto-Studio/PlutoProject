package plutoproject.kernel.api

import java.util.concurrent.ConcurrentHashMap

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
    val stackWalker: StackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
    @Volatile
    var packageOwners: Map<String, String> = emptyMap()
    val modules = ConcurrentHashMap<String, ModuleOwner>()
}

fun currentModuleContextOrNull(): ModuleContext? {
    val moduleId = ModuleContextStorage.stackWalker.walk { frames ->
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
}
