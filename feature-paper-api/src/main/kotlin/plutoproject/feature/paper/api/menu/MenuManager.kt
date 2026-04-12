package plutoproject.feature.paper.api.menu

import plutoproject.feature.paper.api.menu.descriptor.ButtonDescriptor
import plutoproject.feature.paper.api.menu.descriptor.PageDescriptor
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.interactive.ComposableFunction

interface MenuManager {
    companion object : MenuManager by globalKoin.get()

    val pages: List<PageDescriptor>

    fun registerPage(descriptor: PageDescriptor)

    fun registerButton(descriptor: ButtonDescriptor, button: ComposableFunction)

    fun getPageDescriptor(id: String): PageDescriptor?

    fun getButtonDescriptor(id: String): ButtonDescriptor?

    fun getButton(descriptor: ButtonDescriptor): ComposableFunction?
}
