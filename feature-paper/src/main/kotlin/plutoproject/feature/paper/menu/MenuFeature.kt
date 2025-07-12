package plutoproject.feature.paper.menu

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.factory.ButtonDescriptorFactory
import plutoproject.feature.paper.api.menu.factory.PageDescriptorFactory
import plutoproject.feature.paper.menu.factory.ButtonDescriptorFactoryImpl
import plutoproject.feature.paper.menu.factory.PageDescriptorFactoryImpl
import plutoproject.feature.paper.menu.items.MenuItemRecipe
import plutoproject.feature.paper.menu.listeners.ItemListener
import plutoproject.feature.paper.menu.prebuilt.buttons.*
import plutoproject.feature.paper.menu.prebuilt.pages.AssistantPageDescriptor
import plutoproject.feature.paper.menu.prebuilt.pages.HomePageDescriptor
import plutoproject.feature.paper.menu.repositories.UserRepository
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.provider.getCollection
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "menu",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class MenuFeature : PaperFeature(), KoinComponent {
    private val config by inject<MenuConfig>()
    private val featureModule = module {
        single<MenuConfig> { loadConfig(saveConfig()) }
        single<MenuManager> { MenuManagerImpl() }
        single<PageDescriptorFactory> { PageDescriptorFactoryImpl() }
        single<ButtonDescriptorFactory> { ButtonDescriptorFactoryImpl() }
        single<UserRepository> { UserRepository(Provider.getCollection("menu_user_data")) }
        single<PersistMigrator> { PersistMigrator() }
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        AnnotationParser.parse(MenuCommand)
        registerPrebuiltPages()
        registerPrebuiltButtons()
        registerEvents()
        registerItemRecipe()
    }

    private fun registerPrebuiltPages() {
        MenuManager.registerPage(HomePageDescriptor)
        if (config.prebuiltPages.assistant) {
            MenuManager.registerPage(AssistantPageDescriptor)
        }
    }

    private fun registerPrebuiltButtons() {
        if (config.prebuiltButtons.wiki) {
            MenuManager.registerButton(WikiButtonDescriptor) { Wiki() }
        }
        if (config.prebuiltButtons.inspect) {
            MenuManager.registerButton(InspectButtonDescriptor) { Inspect() }
        }
        if (config.prebuiltButtons.balance) {
            MenuManager.registerButton(BalanceButtonDescriptor) { Balance() }
        }
    }

    private fun registerEvents() {
        if (!config.item.enabled) return
        server.pluginManager.registerSuspendingEvents(ItemListener, plugin)
    }

    private fun registerItemRecipe() {
        if (!config.item.registerRecipe) return
        server.addRecipe(MenuItemRecipe)
    }
}
