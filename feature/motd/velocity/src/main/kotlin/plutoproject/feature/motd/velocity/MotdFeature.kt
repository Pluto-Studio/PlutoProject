package plutoproject.feature.motd.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import com.velocitypowered.api.command.CommandSource
import org.incendo.cloud.annotations.AnnotationParser
import plutoproject.capability.legacycloudcommands.api.velocity.VelocityLegacyCloudCommands
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.velocity.VelocityModuleContext

@Feature(
    id = "motd",
    platform = Platform.VELOCITY,
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
class MotdFeature : RuntimeModule {
    private lateinit var service: MotdService
    private var listener: MotdListener? = null
    private var annotationParser: AnnotationParser<CommandSource>? = null
    private val commandRoots = linkedSetOf<String>()

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        service = MotdService(context.saveResource("config.conf"), context.logger)
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as VelocityModuleContext
        listener = MotdListener(service).also {
            context.proxyServer.eventManager.registerSuspend(context.pluginContainer, it)
        }
        registerCommand(context)
    }

    private fun registerCommand(context: VelocityModuleContext) {
        val parser = context.services.getService<VelocityLegacyCloudCommands>().parser
        val manager = parser.manager()
        val rootsBefore = manager.rootCommands().toSet()
        try {
            val commands = parser.parse(MotdCommand(service))
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
