package plutoproject.feature.afk.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.command.CommandSender
import org.bukkit.event.HandlerList
import org.incendo.cloud.annotations.AnnotationParser
import org.koin.dsl.module
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.feature.afk.api.paper.AfkManager
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "afk",
    platform = Platform.PAPER,
    requiredCapabilities = ["legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class AfkFeature : RuntimeModule {
    private var parser: AnnotationParser<CommandSender>? = null
    private val commandRoots = linkedSetOf<String>()

    override suspend fun onLoad(context: ModuleContext) {
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(AfkFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<AfkConfig>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<AfkManager> { AfkManagerImpl() }
        })
        context.services.exportServiceFromKoin<AfkManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, context.plugin)
        val commandParser = context.services.getService<PaperLegacyCloudCommands>().parser
        val rootsBefore = commandParser.manager().rootCommands().toSet()
        try {
            commandParser.parse(AfkCommand)
            commandRoots += commandParser.manager().rootCommands().toSet() - rootsBefore
            parser = commandParser
        } catch (cause: Throwable) {
            (commandParser.manager().rootCommands().toSet() - rootsBefore)
                .forEach(commandParser.manager()::deleteRootCommand)
            throw cause
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        parser?.manager()?.let { manager -> commandRoots.forEach(manager::deleteRootCommand) }
        commandRoots.clear()
        parser = null
        HandlerList.unregisterAll(PlayerListener)
    }
}
