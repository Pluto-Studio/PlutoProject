package plutoproject.platform.paper

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import org.bukkit.plugin.java.JavaPlugin
import plutoproject.kernel.api.Platform
import plutoproject.kernel.common.PreparedRuntimeModules
import plutoproject.kernel.common.RuntimeModulePreparation
import plutoproject.platform.common.resolvePlatformConfig
import java.nio.file.Files

@Suppress("UnstableApiUsage")
class PaperBootstrap : PluginBootstrap {
    private lateinit var prepared: PreparedRuntimeModules

    override fun bootstrap(context: BootstrapContext) {
        Files.createDirectories(context.dataDirectory)
        val configFile = context.dataDirectory.resolve("config.conf")
        if (Files.notExists(configFile)) {
            requireNotNull(javaClass.classLoader.getResourceAsStream("config.conf")) {
                "Platform configuration resource 'config.conf' doesn't exist"
            }.use { Files.copy(it, configFile) }
        }

        prepared = RuntimeModulePreparation.discover(
            platform = Platform.PAPER,
            featureRoots = resolvePlatformConfig(configFile).enableFeatures,
            classLoader = javaClass.classLoader,
        )
        bootstrapModules(context, prepared, javaClass.classLoader)
    }

    fun bootstrapModules(
        context: BootstrapContext,
        prepared: PreparedRuntimeModules,
        classLoader: ClassLoader = PaperBootstrap::class.java.classLoader,
    ) {
        prepared.plan.orderedModules.forEach { descriptor ->
            val className = descriptor.bootstrapEntrypoint ?: return@forEach
            try {
                val entrypoint = Class.forName(className, true, classLoader)
                require(PluginBootstrap::class.java.isAssignableFrom(entrypoint)) {
                    "$className does not implement ${PluginBootstrap::class.qualifiedName}"
                }
                val constructor = entrypoint.getConstructor()
                (constructor.newInstance() as PluginBootstrap).bootstrap(context)
            } catch (cause: Throwable) {
                throw IllegalStateException(
                    "Runtime module '${descriptor.id}' failed during Paper bootstrap",
                    cause,
                )
            }
        }
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin = PaperPlatform(prepared)
}
