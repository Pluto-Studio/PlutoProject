package ink.pmc.menu.api.dsl

import ink.pmc.framework.structure.Builder
import ink.pmc.menu.api.descriptor.ButtonDescriptor
import ink.pmc.menu.api.factory.ButtonDescriptorFactory

class ButtonDescriptorDsl : Builder<ButtonDescriptor> {
    var id: String? = null

    override fun build(): ButtonDescriptor {
        return ButtonDescriptorFactory.create(
            id = id ?: error("Id not set")
        )
    }
}

inline fun buttonDescriptor(block: ButtonDescriptorDsl.() -> Unit): ButtonDescriptor {
    return ButtonDescriptorDsl().apply(block).build()
}