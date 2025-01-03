package ink.pmc.menu.factory

import ink.pmc.menu.api.descriptor.ButtonDescriptor
import ink.pmc.menu.api.factory.ButtonDescriptorFactory
import ink.pmc.menu.descriptor.ButtonDescriptorImpl

class ButtonDescriptorFactoryImpl : ButtonDescriptorFactory {
    override fun create(id: String): ButtonDescriptor {
        return ButtonDescriptorImpl(id)
    }
}