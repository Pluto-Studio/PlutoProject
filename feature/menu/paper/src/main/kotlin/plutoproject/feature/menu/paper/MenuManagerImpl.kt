package plutoproject.feature.menu.paper

import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.menu.api.paper.descriptor.ButtonDescriptor
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor
import plutoproject.capability.interactive.api.ComposableFunction

class MenuManagerImpl : MenuManager {
    private val registeredPages = mutableListOf<PageDescriptor>()
    private val registeredButtons = mutableMapOf<ButtonDescriptor, ComposableFunction>()
    override val pages: List<PageDescriptor>
        get() = registeredPages

    override fun registerPage(descriptor: PageDescriptor) {
        require(!registeredPages.any { it.id == descriptor.id }) { "PageDescriptor for ${descriptor.id} already registered" }
        registeredPages.add(descriptor)
    }

    override fun registerButton(descriptor: ButtonDescriptor, button: ComposableFunction) {
        require(!registeredButtons.keys.any { it.id == descriptor.id }) { "ButtonDescriptor for ${descriptor.id} already registered" }
        registeredButtons[descriptor] = button
    }

    override fun getPageDescriptor(id: String): PageDescriptor? {
        return registeredPages.firstOrNull { it.id == id }
    }

    override fun getButtonDescriptor(id: String): ButtonDescriptor? {
        return registeredButtons.keys.firstOrNull { it.id == id }
    }

    override fun getButton(descriptor: ButtonDescriptor): ComposableFunction? {
        return registeredButtons[descriptor]
    }
}
