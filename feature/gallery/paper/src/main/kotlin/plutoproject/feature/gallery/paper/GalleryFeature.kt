package plutoproject.feature.gallery.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.incendo.cloud.annotations.AnnotationParser
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.serveridentifier.api.ServerIdentifier
import plutoproject.feature.gallery.api.GalleryService
import plutoproject.feature.gallery.common.*
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.paper.listener.ChunkListener
import plutoproject.feature.gallery.paper.listener.CraftListener
import plutoproject.feature.gallery.paper.listener.ItemFrameListener
import plutoproject.feature.gallery.paper.listener.PlayerListener
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

@Feature(
    id = "gallery",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo", "server_identifier", "interactive", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class GalleryFeature : RuntimeModule {
    private var annotationParser: AnnotationParser<CommandSender>? = null
    private val commandRoots = linkedSetOf<String>()
    private var registeredListeners: List<Listener> = emptyList()

    override suspend fun onLoad(context: ModuleContext) {
        context as PaperModuleContext
        context.dataFolder.toFile().mkdirs()
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(GalleryFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<GalleryConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<ServerIdentifier>()
        context.importServiceToKoin<GuiManager>()
        context.loadKoinModuleDefinitions(
            module {
                single { config }
                single<CoroutineScope> { context.coroutineScope }
                single<CoroutineContext> { context.coroutineScope.coroutineContext }
                single<Server> { context.plugin.server }
                single<Plugin> { context.plugin }
                single<Logger> { context.logger }
                singleOf(::PaperViewPort) bind ViewPort::class
                singleOf(::PaperMapUpdatePort) bind MapUpdatePort::class
                single<DisplayInstanceIndex> {
                    PaperDisplayInstanceIndex(get(), get(), context.plugin.minecraftDispatcher)
                }
            },
            commonModule,
        )
        context.services.exportServiceFromKoin<GalleryService>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        onFeatureEnable(context.koin)

        // TODO(runtime-module): Restore after menu exposes a new feature API.
        // if (isMenuAvailable) {
        //     MenuManager.registerButton(ImageListMenuButtonDescriptor) { ImageListMenuButton() }
        // }

        registerCommands(context)
        registerImageItemCopyRecipe(context.plugin.server)
        registeredListeners = listOf(PlayerListener, ChunkListener, ItemFrameListener, CraftListener)
            .onEach { context.plugin.server.pluginManager.registerSuspendingEvents(it, context.plugin) }
    }

    private fun registerCommands(context: PaperModuleContext) {
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        val manager = parser.manager()
        val rootsBefore = manager.rootCommands().toSet()

        try {
            val commands = parser.parse(
                GalleyDebugCommand,
                GalleryCancelUploadCommand,
                GalleryMigrateImageDataCommand
            )
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
        annotationParser?.manager()?.let { manager ->
            commandRoots.forEach(manager::deleteRootCommand)
        }
        commandRoots.clear()
        annotationParser = null

        registeredListeners.forEach(HandlerList::unregisterAll)
        registeredListeners = emptyList()
        context.koinGet<Server>().removeRecipe(IMAGE_ITEM_COPY_RECIPE_KEY)
        onFeatureDisable(context.koin)
    }
}
