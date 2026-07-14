package plutoproject.feature.menu.paper.factory

import plutoproject.feature.menu.api.paper.descriptor.ButtonDescriptor
import plutoproject.feature.menu.api.paper.factory.ButtonDescriptorFactory
import plutoproject.feature.menu.paper.descriptor.ButtonDescriptorImpl

class ButtonDescriptorFactoryImpl : ButtonDescriptorFactory {
    override fun create(id: String): ButtonDescriptor {
        return ButtonDescriptorImpl(id)
    }
}
