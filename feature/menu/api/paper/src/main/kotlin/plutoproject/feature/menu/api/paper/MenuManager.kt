package plutoproject.feature.menu.api.paper

import plutoproject.feature.menu.api.paper.descriptor.ButtonDescriptor
import plutoproject.feature.menu.api.paper.descriptor.PageDescriptor
import plutoproject.capability.interactive.api.ComposableFunction

interface MenuManager {
    val pages: List<PageDescriptor>

    fun registerPage(descriptor: PageDescriptor)

    fun registerButton(descriptor: ButtonDescriptor, button: ComposableFunction)

    fun getPageDescriptor(id: String): PageDescriptor?

    fun getButtonDescriptor(id: String): ButtonDescriptor?

    fun getButton(descriptor: ButtonDescriptor): ComposableFunction?
}
