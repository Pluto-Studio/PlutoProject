package plutoproject.kernel.common

import kotlin.reflect.KClass
import org.koin.core.Koin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import plutoproject.kernel.api.ModuleServices
import plutoproject.kernel.api.ServiceRegistration

internal class RuntimeServiceRegistry {
    private data class Export(val owner: String, val instance: Any, val token: Any)
    private enum class OwnerState { INITIALIZING, ACTIVE, CLOSING, CLOSED }

    private val lock = Any()
    private val exports = mutableMapOf<KClass<*>, Export>()

    fun owner(owner: String, koin: Koin) = OwnerServices(owner, koin)

    internal inner class OwnerServices(
        private val owner: String,
        private val koin: Koin,
    ) : ModuleServices {
        private var state = OwnerState.INITIALIZING

        fun activate() = synchronized(lock) {
            check(state == OwnerState.INITIALIZING) { "Module '$owner' services cannot be activated from $state" }
            state = OwnerState.ACTIVE
        }

        fun beginClosing() = synchronized(lock) {
            if (state == OwnerState.CLOSED || state == OwnerState.CLOSING) return@synchronized
            state = OwnerState.CLOSING
            exports.entries.removeIf { it.value.owner == owner }
        }

        fun close() = synchronized(lock) {
            exports.entries.removeIf { it.value.owner == owner }
            state = OwnerState.CLOSED
        }

        override fun <T : Any> exportService(type: KClass<T>, instance: T): ServiceRegistration = synchronized(lock) {
            requireActive("export services")
            check(type.isInstance(instance)) { "Service instance does not implement ${type.qualifiedName}" }
            check(type !in exports) { "Service ${type.qualifiedName} already has an active provider" }
            val token = Any()
            exports[type] = Export(owner, instance, token)
            Registration(type, token)
        }

        override fun <T : Any> exportService(
            type: KClass<T>,
            qualifier: Qualifier?,
            parameters: ParametersDefinition?,
        ): ServiceRegistration = exportService(
            type,
            koin.get<T>(clazz = type, qualifier = qualifier, parameters = parameters),
        )

        override fun <T : Any> getService(type: KClass<T>): T = synchronized(lock) {
            requireQueryable()
            val instance = exports[type]?.instance
                ?: error("No active service provider for ${type.qualifiedName}")
            @Suppress("UNCHECKED_CAST")
            instance as T
        }

        override fun <T : Any> getServiceOrNull(type: KClass<T>): T? = synchronized(lock) {
            requireQueryable()
            @Suppress("UNCHECKED_CAST")
            exports[type]?.instance as T?
        }

        private fun requireActive(operation: String) {
            check(state == OwnerState.ACTIVE) { "Module '$owner' cannot $operation while services are $state" }
        }

        private fun requireQueryable() {
            check(state != OwnerState.CLOSED) { "Module '$owner' services are closed" }
        }

        private inner class Registration(
            override val serviceType: KClass<*>,
            private val token: Any,
        ) : ServiceRegistration {
            override val isRegistered: Boolean
                get() = synchronized(lock) { exports[serviceType]?.token === token }

            override fun unregister() = synchronized(lock) {
                requireActive("unregister services")
                if (exports[serviceType]?.token === token) exports.remove(serviceType)
                Unit
            }
        }
    }
}
