package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.RuntimeModule

fun interface RuntimeModuleFactory {
    fun create(descriptor: ModuleDescriptor): RuntimeModule
}

class ReflectiveRuntimeModuleFactory(
    private val classLoader: ClassLoader = ReflectiveRuntimeModuleFactory::class.java.classLoader,
) : RuntimeModuleFactory {
    override fun create(descriptor: ModuleDescriptor): RuntimeModule {
        val entrypoint = Class.forName(descriptor.entrypoint, true, classLoader)
        require(RuntimeModule::class.java.isAssignableFrom(entrypoint)) {
            "${descriptor.entrypoint} does not implement ${RuntimeModule::class.qualifiedName}"
        }
        val constructor = entrypoint.getConstructor()
        return constructor.newInstance() as RuntimeModule
    }
}
