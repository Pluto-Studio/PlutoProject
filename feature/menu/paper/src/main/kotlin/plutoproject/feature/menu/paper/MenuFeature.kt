package plutoproject.feature.menu.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.incendo.cloud.annotations.AnnotationParser
import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.legacycloudcommands.api.paper.PaperLegacyCloudCommands
import plutoproject.capability.mongo.api.MongoConnection
import plutoproject.capability.mongo.api.getCollection
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.menu.api.paper.factory.ButtonDescriptorFactory
import plutoproject.feature.menu.api.paper.factory.PageDescriptorFactory
import plutoproject.feature.menu.paper.factory.ButtonDescriptorFactoryImpl
import plutoproject.feature.menu.paper.factory.PageDescriptorFactoryImpl
import plutoproject.feature.menu.paper.items.MenuItemRecipe
import plutoproject.feature.menu.paper.listeners.ItemListener
import plutoproject.feature.menu.paper.prebuilt.buttons.*
import plutoproject.feature.menu.paper.prebuilt.pages.AssistantPageDescriptor
import plutoproject.feature.menu.paper.prebuilt.pages.HomePageDescriptor
import plutoproject.feature.menu.paper.repositories.UserRepository
import plutoproject.foundation.paper.command.CloudCommandRegistration
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "menu",
    platform = Platform.PAPER,
    requiredCapabilities = ["mongo", "database_persist", "interactive", "legacy_cloud_commands"],
)
@Suppress("UNUSED")
class MenuFeature : RuntimeModule {
    private var commands: CloudCommandRegistration? = null
    private var itemListenerRegistered = false
    private var itemRecipeRegistered = false

    override suspend fun onLoad(context: ModuleContext) {
        val paper = context as PaperModuleContext
        val configFile = context.saveResource("config.conf")
        val config = ConfigLoaderBuilder.empty()
            .withClassLoader(MenuFeature::class.java.classLoader)
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<MenuConfig>()

        context.importServiceToKoin<MongoConnection>()
        context.importServiceToKoin<DatabasePersist>()
        context.importServiceToKoin<GuiManager>()
        context.loadKoinModuleDefinitions(module {
            single { config }
            single<MenuManager> { MenuManagerImpl() }
            single<PageDescriptorFactory> { PageDescriptorFactoryImpl() }
            single<ButtonDescriptorFactory> { ButtonDescriptorFactoryImpl() }
            single { UserRepository(get<MongoConnection>().getCollection("menu_user_data")) }
            single { PersistMigrator() }
        })
        context.services.exportServiceFromKoin<MenuManager>()
        context.services.exportServiceFromKoin<PageDescriptorFactory>()
        context.services.exportServiceFromKoin<ButtonDescriptorFactory>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        val paper = context as PaperModuleContext
        val config = context.koinGet<MenuConfig>()
        val manager = context.koinGet<MenuManager>()
        val parser = context.services.getService<PaperLegacyCloudCommands>().parser
        commands = CloudCommandRegistration.register(parser, MenuCommand)

        manager.registerPage(HomePageDescriptor)
        if (config.prebuiltPages.assistant) manager.registerPage(AssistantPageDescriptor)
        if (config.prebuiltButtons.wiki) manager.registerButton(WikiButtonDescriptor) { Wiki() }
        if (config.prebuiltButtons.inspect) manager.registerButton(InspectButtonDescriptor) { Inspect() }
        if (config.prebuiltButtons.balance) manager.registerButton(BalanceButtonDescriptor) { Balance() }

        if (config.item.enabled) {
            paper.plugin.server.pluginManager.registerSuspendingEvents(ItemListener, paper.plugin)
            itemListenerRegistered = true
        }
        if (config.item.registerRecipe) {
            paper.plugin.server.addRecipe(MenuItemRecipe)
            itemRecipeRegistered = true
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        commands?.close()
        commands = null
        if (itemListenerRegistered) HandlerList.unregisterAll(ItemListener)
        itemListenerRegistered = false
        if (itemRecipeRegistered) (context as PaperModuleContext).plugin.server.removeRecipe(MenuItemRecipe.key)
        itemRecipeRegistered = false
    }
}
