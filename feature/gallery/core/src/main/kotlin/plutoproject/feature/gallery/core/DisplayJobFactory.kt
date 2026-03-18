package plutoproject.feature.gallery.core

interface DisplayJobFactory {
    fun create(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): DisplayJob
}
