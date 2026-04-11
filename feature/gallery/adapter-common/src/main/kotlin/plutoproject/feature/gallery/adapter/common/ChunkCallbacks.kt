package plutoproject.feature.gallery.adapter.common

import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.feature.gallery.core.util.ChunkKey
import java.util.logging.Level
import java.util.logging.Logger

private val logger = koin.get<Logger>()
private val index = koin.get<DisplayInstanceIndex>()
private val imageStore = koin.get<ImageStore>()
private val imageDataStore = koin.get<ImageDataStore>()
private val displayInstanceStore = koin.get<DisplayInstanceStore>()
private val displayRuntime = koin.get<DisplayRuntimeRegistry>()

suspend fun chunkLoad(world: String, chunk: ChunkKey) {
    val ids = index.get(world, chunk)
    if (ids.isEmpty()) {
        return
    }

    val instances = displayInstanceStore.getMany(ids)
    if (instances.isEmpty()) {
        return
    }

    val imageIds = instances.values.map { it.imageId }
    val images = imageStore.getMany(imageIds)
    val imageData = imageDataStore.getMany(imageIds)

    val invalidImages = images.filter { (id, _) -> !imageData.containsKey(id) }.values
    val invalidImageString = invalidImages.joinToString(", ") { "\"${it.name}\"" }
    if (invalidImages.isNotEmpty()) {
        logger.log(Level.WARNING, "Found ${invalidImages.size} image(s) without corresponding data: $invalidImageString")
    }

    instances.forEach { (_, instance) ->
        val image = images[instance.imageId] ?: return@forEach
        val imageData = imageData[instance.imageId] ?: return@forEach
        displayRuntime.attach(image, imageData, instance)
    }
}

suspend fun chunkUnload(world: String, chunk: ChunkKey) {
    val ids = index.get(world, chunk)
    if (ids.isEmpty()) {
        return
    }

    // TODO: 本地 Chunk 索引存储 Image ID，避免卸载也走数据库加载一次
    val instances = displayInstanceStore.getMany(ids)
    if (instances.isEmpty()) {
        return
    }

    // TODO: DisplayRuntime 批量卸载？
    instances.forEach { (_, instance) ->
        displayRuntime.detach(instance.imageId, instance.id)
    }
}
