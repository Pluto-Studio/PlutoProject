package plutoproject.kernel.api

import org.koin.core.Koin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.module.Module

inline fun <reified T : Any> ModuleContext.injectModule(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null,
): Lazy<T> = lazy(mode) { getModule(qualifier, parameters) }

inline fun <reified T : Any> injectModule(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null,
): Lazy<T> = currentModuleContext().injectModule(qualifier, mode, parameters)

inline fun <reified T : Any> ModuleContext.getModule(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = koin.get(qualifier, parameters)

inline fun <reified T : Any> getModule(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = currentModuleContext().getModule(qualifier, parameters)

fun ModuleContext.loadModuleDefinitions(
    modules: List<Module>,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) {
    koin.loadModules(modules, allowOverride, createEagerInstances)
}

fun ModuleContext.loadModuleDefinitions(
    module: Module,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) = loadModuleDefinitions(listOf(module), allowOverride, createEagerInstances)

fun loadModuleDefinitions(
    modules: List<Module>,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) = currentModuleContext().loadModuleDefinitions(modules, allowOverride, createEagerInstances)

fun loadModuleDefinitions(
    module: Module,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) = currentModuleContext().loadModuleDefinitions(module, allowOverride, createEagerInstances)

fun ModuleContext.unloadModuleDefinitions(modules: List<Module>) = koin.unloadModules(modules)

fun ModuleContext.unloadModuleDefinitions(module: Module) = unloadModuleDefinitions(listOf(module))

fun unloadModuleDefinitions(modules: List<Module>) = currentModuleContext().unloadModuleDefinitions(modules)

fun unloadModuleDefinitions(module: Module) = currentModuleContext().unloadModuleDefinitions(module)
