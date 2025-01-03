package ink.pmc.menu.api

import ink.pmc.framework.interactive.ComposableFunction
import ink.pmc.framework.inject.inlinedGet
import ink.pmc.menu.api.descriptor.ButtonDescriptor
import ink.pmc.menu.api.descriptor.PageDescriptor

interface MenuManager {
    companion object : MenuManager by inlinedGet()

    val pages: List<PageDescriptor>

    fun registerPage(descriptor: PageDescriptor)

    fun registerButton(descriptor: ButtonDescriptor, button: ComposableFunction)

    fun getPageDescriptor(id: String): PageDescriptor?

    fun getButtonDescriptor(id: String): ButtonDescriptor?

    fun getButton(descriptor: ButtonDescriptor): ComposableFunction?
}