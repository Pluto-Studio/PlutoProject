package plutoproject.kernel.api

import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> ModuleContext.koinInject(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null,
): Lazy<T> = lazy(mode) { koinGet(qualifier, parameters) }

inline fun <reified T : Any> koinInject(
    qualifier: Qualifier? = null,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    noinline parameters: ParametersDefinition? = null,
): Lazy<T> = currentModuleContext().koinInject(qualifier, mode, parameters)

inline fun <reified T : Any> ModuleContext.koinGet(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = koin.get(qualifier, parameters)

inline fun <reified T : Any> koinGet(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = currentModuleContext().koinGet(qualifier, parameters)

fun ModuleContext.loadKoinModuleDefinitions(
    vararg modules: Module,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) {
    loadKoinModuleDefinitions(modules.toList(), allowOverride, createEagerInstances)
}

fun ModuleContext.loadKoinModuleDefinitions(
    modules: List<Module>,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) {
    koin.loadModules(modules, allowOverride, createEagerInstances)
}

fun loadKoinModuleDefinitions(
    vararg modules: Module,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) = currentModuleContext().loadKoinModuleDefinitions(modules.toList(), allowOverride, createEagerInstances)

fun loadKoinModuleDefinitions(
    modules: List<Module>,
    allowOverride: Boolean = true,
    createEagerInstances: Boolean = true,
) = currentModuleContext().loadKoinModuleDefinitions(modules, allowOverride, createEagerInstances)

fun ModuleContext.unloadKoinModuleDefinitions(modules: List<Module>) = koin.unloadModules(modules)

fun ModuleContext.unloadKoinModuleDefinitions(module: Module) = unloadKoinModuleDefinitions(listOf(module))

fun unloadKoinModuleDefinitions(modules: List<Module>) = currentModuleContext().unloadKoinModuleDefinitions(modules)

fun unloadKoinModuleDefinitions(module: Module) = currentModuleContext().unloadKoinModuleDefinitions(module)
