package plutoproject.feature.gallery.core

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ImageManager {
    private val loadedImages = ConcurrentHashMap<UUID, Image>()
    private val loadedImageDataEntries = ConcurrentHashMap<UUID, ImageDataEntry<*>>()

    fun getLoadedImage(id: UUID): Image? {
        return loadedImages[id]
    }

    fun loadImage(image: Image): Image {
        loadedImages[image.id] = image
        return image
    }

    fun unloadImage(id: UUID): Image? {
        return loadedImages.remove(id)
    }

    suspend fun getImage(id: UUID, loader: suspend (UUID) -> Image?): Image? {
        val loaded = loadedImages[id]
        if (loaded != null) {
            return loaded
        }

        val image = loader(id) ?: return null
        loadedImages[image.id] = image
        return image
    }

    fun getLoadedImageDataEntry(belongsTo: UUID): ImageDataEntry<*>? {
        return loadedImageDataEntries[belongsTo]
    }

    fun loadImageDataEntry(entry: ImageDataEntry<*>): ImageDataEntry<*> {
        loadedImageDataEntries[entry.belongsTo] = entry
        return entry
    }

    fun unloadImageDataEntry(belongsTo: UUID): ImageDataEntry<*>? {
        return loadedImageDataEntries.remove(belongsTo)
    }

    suspend fun getImageDataEntry(
        belongsTo: UUID,
        loader: suspend (UUID) -> ImageDataEntry<*>?,
    ): ImageDataEntry<*>? {
        val loaded = loadedImageDataEntries[belongsTo]
        if (loaded != null) {
            return loaded
        }

        val entry = loader(belongsTo) ?: return null
        loadedImageDataEntries[entry.belongsTo] = entry
        return entry
    }
}
