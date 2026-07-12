package plutoproject.kernel.api

import kotlin.reflect.KClass
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module

interface ServiceRegistration {
    val serviceType: KClass<*>
    val isRegistered: Boolean

    fun unregister()
}

interface ModuleServices {
    fun <T : Any> exportService(type: KClass<T>, instance: T): ServiceRegistration

    fun <T : Any> exportService(
        type: KClass<T>,
        qualifier: Qualifier? = null,
        parameters: ParametersDefinition? = null,
    ): ServiceRegistration

    fun <T : Any> getService(type: KClass<T>): T

    fun <T : Any> getServiceOrNull(type: KClass<T>): T?
}

inline fun <reified T : Any> ModuleServices.exportService(instance: T): ServiceRegistration =
    exportService(T::class, instance)

inline fun <reified T : Any> ModuleServices.exportService(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): ServiceRegistration = exportService(T::class, qualifier, parameters)

inline fun <reified T : Any> ModuleServices.getService(): T = getService(T::class)

inline fun <reified T : Any> ModuleServices.getServiceOrNull(): T? = getServiceOrNull(T::class)

inline fun <reified T : Any> ModuleContext.importService(qualifier: Qualifier? = null): Module {
    val definitions = module { factory(qualifier) { services.getService<T>() } }
    loadKoinModuleDefinitions(definitions)
    return definitions
}
