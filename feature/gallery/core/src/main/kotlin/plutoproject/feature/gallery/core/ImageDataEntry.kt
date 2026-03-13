package plutoproject.feature.gallery.core

import java.util.*

class ImageDataEntry<T : Any>(
    val belongsTo: UUID,
    val type: ImageType,
    data: T
) {
    var data: T = data
        private set

    init {
        checkImageData(data)
    }

    internal fun replaceData(newData: T) {
        checkImageData(newData)
        data = newData
    }

    fun asStaticData(): StaticImageData {
        require(type == ImageType.STATIC && data is StaticImageData) { "Image data type mismatch" }
        return data as StaticImageData
    }

    fun asAnimatedData(): AnimatedImageData {
        require(type == ImageType.ANIMATED && data is AnimatedImageData) { "Image data type mismatch" }
        return data as AnimatedImageData
    }

    private fun checkImageData(data: T) = when (type) {
        ImageType.STATIC -> require(data is StaticImageData) {
            "Image data type mismatch: expected StaticImageData, got ${data::class.simpleName}"
        }

        ImageType.ANIMATED -> require(data is AnimatedImageData) {
            "Image data type mismatch: expected AnimatedImageData, got ${data::class.simpleName}"
        }
    }
}
