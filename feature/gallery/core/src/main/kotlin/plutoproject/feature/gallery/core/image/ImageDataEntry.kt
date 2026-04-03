package plutoproject.feature.gallery.core.image

import java.util.*

sealed class ImageDataEntry<T : Any> {
    abstract val imageId: UUID
    abstract val type: ImageType
    abstract var data: T
        protected set

    class Static(
        override val imageId: UUID,
        override var data: ImageData.Static
    ) : ImageDataEntry<ImageData.Static>() {
        override val type: ImageType = ImageType.STATIC
    }

    class Animated(
        override val imageId: UUID,
        override var data: ImageData.Animated
    ) : ImageDataEntry<ImageData.Animated>() {
        override val type: ImageType = ImageType.ANIMATED
    }

    internal fun replaceData(newData: T) {
        data = newData
    }

    fun isStatic(): Boolean = this is Static

    fun isAnimated(): Boolean = this is Animated

    fun asStaticOrNull(): Static? = this as? Static

    fun asAnimatedOrNull(): Animated? = this as? Animated

    fun asStatic(): Static = asStaticOrNull() ?: error("Type mismatch, expected static")

    fun asAnimated(): Animated = asAnimatedOrNull() ?: error("Type mismatch, expected animated")
}
