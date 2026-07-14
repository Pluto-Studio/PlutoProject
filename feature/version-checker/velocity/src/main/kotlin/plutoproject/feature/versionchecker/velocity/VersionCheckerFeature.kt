package plutoproject.feature.versionchecker.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.legacycloudcommands.api.velocity.VelocityLegacyCloudCommands
import plutoproject.foundation.velocity.command.CloudCommandRegistration
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Feature(
    id = "version_checker",
    platform = Platform.VELOCITY,
    requiredCapabilities = ["database_persist", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class VersionCheckerFeature : RuntimeModule {
    private lateinit var config: VersionCheckerConfig
    private var listener: PingListener? = null
    private var commands: CloudCommandRegistration? = null

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        config = ConfigLoaderBuilder.empty()
            .withClassLoader(VersionCheckerFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addDecoder(IntRangeDecoder)
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        val databasePersist = context.services.getService<DatabasePersist>()
        listener = PingListener(config, databasePersist).also {
            context.proxyServer.eventManager.registerSuspend(context.pluginContainer, it)
        }
        if (config.enableVersionWarning) {
            registerCommand(context, databasePersist)
        }
    }

    private fun registerCommand(context: VelocityModuleContext, databasePersist: DatabasePersist) {
        val parser = context.services.getService<VelocityLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, IgnoreCommand(config, databasePersist))
    }

    override suspend fun onDisable(context: ModuleContext) {
        context as VelocityModuleContext
        commands?.close()
        commands = null
        listener?.let { context.proxyServer.eventManager.unregisterListener(context.pluginContainer, it) }
        listener = null
    }
}
