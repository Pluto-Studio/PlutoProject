package plutoproject.feature.menu.api.paper.factory

import plutoproject.feature.menu.api.paper.descriptor.ButtonDescriptor

interface ButtonDescriptorFactory {
    fun create(id: String): ButtonDescriptor
}
