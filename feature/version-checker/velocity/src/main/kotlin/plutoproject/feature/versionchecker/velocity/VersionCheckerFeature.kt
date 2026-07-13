package plutoproject.feature.versionchecker.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.AnnotationParser
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.legacycloudcommands.api.velocity.VelocityLegacyCloudCommands
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
    private var annotationParser: AnnotationParser<CommandSource>? = null
    private val commandRoots = linkedSetOf<String>()

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
        val manager = parser.manager()
        val rootsBefore = manager.rootCommands().toSet()
        try {
            val commands = parser.parse(IgnoreCommand(config, databasePersist))
            val parsedRoots = commands.flatMap { command ->
                val root = command.rootComponent()
                listOf(root.name()) + root.aliases() + root.alternativeAliases()
            }.toSet()
            commandRoots += parsedRoots.intersect(manager.rootCommands().toSet() - rootsBefore)
            annotationParser = parser
        } catch (cause: Throwable) {
            (manager.rootCommands().toSet() - rootsBefore).forEach(manager::deleteRootCommand)
            throw cause
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        context as VelocityModuleContext
        annotationParser?.manager()?.let { manager -> commandRoots.forEach(manager::deleteRootCommand) }
        commandRoots.clear()
        annotationParser = null
        listener?.let { context.proxyServer.eventManager.unregisterListener(context.pluginContainer, it) }
        listener = null
    }
}
