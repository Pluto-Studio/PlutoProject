package plutoproject.kernel.common

import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.Platform

data class PreparedRuntimeModules(
    val descriptors: List<ModuleDescriptor>,
    val packageOwners: Map<String, String>,
    val plan: ActivationPlan,
)

object RuntimeModulePreparation {
    fun discover(
        platform: Platform,
        featureRoots: Collection<String>,
        classLoader: ClassLoader,
    ): PreparedRuntimeModules {
        val discovery = ModuleDiscovery(classLoader).discover(platform)
        require(discovery.errors.isEmpty()) {
            discovery.errors.joinToString("\n") { error ->
                "${error.source}: ${error.message}${error.cause?.message?.let { ": $it" }.orEmpty()}"
            }
        }
        return prepare(
            platform = platform,
            descriptors = discovery.modules.map(DiscoveredModule::descriptor),
            featureRoots = featureRoots,
            packageOwners = discovery.packageOwners,
        )
    }

    fun prepare(
        platform: Platform,
        descriptors: Collection<ModuleDescriptor>,
        featureRoots: Collection<String>,
        packageOwners: Map<String, String> = emptyMap(),
    ): PreparedRuntimeModules {
        val validatedDescriptors = ModuleDescriptorValidator.validateForPlatform(platform, descriptors)
        val graph = ModuleGraph(validatedDescriptors)
        return PreparedRuntimeModules(
            descriptors = validatedDescriptors,
            packageOwners = packageOwners.toMap(),
            plan = ModulePlanner(graph).plan(featureRoots),
        )
    }
}
