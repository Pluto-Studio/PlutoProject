package plutoproject.feature.menu.api.paper.dsl

import plutoproject.feature.menu.api.paper.descriptor.ButtonDescriptor
import plutoproject.feature.menu.api.paper.factory.ButtonDescriptorFactory
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getService

class ButtonDescriptorDsl {
    var id: String? = null

    fun build(): ButtonDescriptor = currentModuleContext().services.getService<ButtonDescriptorFactory>().create(
        id = id ?: error("Id not set")
    )
}

inline fun ButtonDescriptor(block: ButtonDescriptorDsl.() -> Unit): ButtonDescriptor =
    ButtonDescriptorDsl().apply(block).build()
