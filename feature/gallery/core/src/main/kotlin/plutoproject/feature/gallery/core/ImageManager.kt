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

    fun getLoadedImages(ids: Collection<UUID>): Map<UUID, Image> {
        if (ids.isEmpty()) {
            return emptyMap()
        }

        return ids.mapNotNull { id ->
            loadedImages[id]?.let { id to it }
        }.toMap()
    }

    fun loadImages(images: Collection<Image>) {
        images.forEach(::loadImage)
    }

    fun unloadImage(id: UUID): Image? {
        return loadedImages.remove(id)
    }

    fun unloadImages(ids: Collection<UUID>): List<Image> {
        return ids.mapNotNull(::unloadImage)
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

    fun getLoadedImageDataEntries(belongsToList: Collection<UUID>): Map<UUID, ImageDataEntry<*>> {
        if (belongsToList.isEmpty()) {
            return emptyMap()
        }

        return belongsToList.mapNotNull { belongsTo ->
            loadedImageDataEntries[belongsTo]?.let { belongsTo to it }
        }.toMap()
    }

    fun loadImageDataEntries(entries: Collection<ImageDataEntry<*>>) {
        entries.forEach(::loadImageDataEntry)
    }

    fun unloadImageDataEntry(belongsTo: UUID): ImageDataEntry<*>? {
        return loadedImageDataEntries.remove(belongsTo)
    }

    fun unloadImageDataEntries(belongsToList: Collection<UUID>): List<ImageDataEntry<*>> {
        return belongsToList.mapNotNull(::unloadImageDataEntry)
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
