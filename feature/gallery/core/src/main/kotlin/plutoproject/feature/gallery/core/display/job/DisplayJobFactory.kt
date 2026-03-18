package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry

interface DisplayJobFactory {
    fun create(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): DisplayJob
}
