package ink.pmc.menu.api.factory

import ink.pmc.framework.inject.inlinedGet
import ink.pmc.menu.api.descriptor.ButtonDescriptor

interface ButtonDescriptorFactory {
    companion object : ButtonDescriptorFactory by inlinedGet()

    fun create(id: String): ButtonDescriptor
}