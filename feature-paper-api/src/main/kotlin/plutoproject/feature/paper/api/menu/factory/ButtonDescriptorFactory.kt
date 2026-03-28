package plutoproject.feature.paper.api.menu.factory

import plutoproject.feature.paper.api.menu.descriptor.ButtonDescriptor
import plutoproject.framework.common.util.inject.globalKoin

interface ButtonDescriptorFactory {
    companion object : ButtonDescriptorFactory by globalKoin.get()

    fun create(id: String): ButtonDescriptor
}
