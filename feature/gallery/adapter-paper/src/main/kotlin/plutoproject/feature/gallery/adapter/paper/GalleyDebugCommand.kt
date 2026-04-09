package plutoproject.feature.gallery.adapter.paper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.gallery.adapter.common.*
import plutoproject.feature.gallery.core.AllocateMapIdUseCase
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.UnifiedImageDecoder
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.feature.gallery.core.render.*
import plutoproject.framework.common.util.coroutine.Loom
import java.util.*
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val PERMISSION_GALLERY_DEBUG = "plutoproject.gallery.command.gallery.debug"
private const val DEFAULT_BACKGROUND_RGB24 = 0x000000

@Suppress("UNUSED")
object GalleyDebugCommand {
    private val httpClient = OkHttpClient()
    private val galleryConfig = koin.get<GalleryConfig>()
    private val allocateMapIdUseCase = koin.get<AllocateMapIdUseCase>()
    private val imageStore = koin.get<ImageStore>()
    private val imageDataStore = koin.get<ImageDataStore>()
    private val displayInstanceStore = koin.get<DisplayInstanceStore>()
    private val displayRuntime = koin.get<DisplayRuntimeRegistry>()

    @Command("gallery debug create <name> <url> <width> <height> [repositionMode] [scaleMode] [quantizeMode] [ditherMode]")
    @Permission(PERMISSION_GALLERY_DEBUG)
    suspend fun CommandSender.create(
        @Argument("name") @Quoted name: String,
        @Argument("url") @Quoted url: String,
        @Argument("width") width: Int,
        @Argument("height") height: Int,
        @Argument("repositionMode") repositionMode: RepositionMode?,
        @Argument("scaleMode") scaleMode: ScaleMode?,
        @Argument("quantizeMode") quantizeMode: QuantizeMode?,
        @Argument("ditherMode") ditherMode: DitherMode?,
    ) {
        val player = this as? Player
        if (player == null) {
            sendMessage("Create failed: this command must be executed by a player because it gives the created image item to the executor.")
            return
        }

        if (name.isBlank()) {
            player.sendMessage("Create failed: image name must not be blank.")
            return
        }
        if (width <= 0 || height <= 0) {
            player.sendMessage("Create failed: width and height must both be > 0. width=$width, height=$height")
            return
        }

        val mapCount = runCatching { Math.multiplyExact(width, height) }
            .getOrElse {
                player.sendMessage("Create failed: width * height overflowed Int. width=$width, height=$height")
                return
            }

        val effectiveRepositionMode = repositionMode ?: galleryConfig.render.defaultRepositionMode
        val effectiveScaleMode = scaleMode ?: galleryConfig.render.defaultScaleMode
        val effectiveQuantizeMode = quantizeMode ?: galleryConfig.render.defaultQuantizeMode
        val effectiveDitherMode = ditherMode ?: galleryConfig.render.defaultDitherMode

        val renderComponents = RenderComponents(
            repositioner = effectiveRepositionMode.repositioner,
            scaler = effectiveScaleMode.scaler,
            quantizer = effectiveQuantizeMode.quantizer,
            ditherer = effectiveDitherMode.ditherer,
        )

        val createResult = withContext(Dispatchers.Loom) {
            createImageFromUrl(
                owner = player,
                name = name,
                url = url,
                width = width,
                height = height,
                mapCount = mapCount,
                renderComponents = renderComponents,
            )
        }

        when (createResult) {
            is CreateImageDebugResult.Failure -> {
                player.sendMessage(createResult.message)
            }

            is CreateImageDebugResult.Success -> {
                val droppedItemCount = giveItem(player, createImageItem(createResult.image))
                player.sendMessage(
                    "Create succeeded. imageId=${createResult.image.id}, type=${createResult.image.type}, name=${
                        quote(
                            createResult.image.name
                        )
                    }, url=${quote(url)}, " +
                            "downloadedBytes=${createResult.downloadedBytes}, size=${width}x${height}, mapCount=$mapCount, uniqueTileCount=${createResult.imageData.tilePool.tileCount}, " +
                            "modes={reposition=$effectiveRepositionMode, scale=$effectiveScaleMode, quantize=$effectiveQuantizeMode, dither=$effectiveDitherMode}, droppedItemCount=$droppedItemCount"
                )
            }
        }
    }

    @Command("gallery debug delete <imageId>")
    @Permission(PERMISSION_GALLERY_DEBUG)
    suspend fun CommandSender.delete(@Argument("imageId") imageIdRaw: String) {
        val imageId = imageIdRaw.toUuidOrNull()
        if (imageId == null) {
            sendMessage("Delete failed: imageId is not a valid UUID. imageId=${quote(imageIdRaw)}")
            return
        }

        val image = withContext(Dispatchers.Loom) {
            imageStore.get(imageId)
        }
        if (image == null) {
            sendMessage("Delete failed: image does not exist. imageId=$imageId")
            return
        }

        val instances = withContext(Dispatchers.Loom) {
            displayInstanceStore.findByImageId(imageId)
        }
        val cleanupSummary = cleanupDisplayInstances(imageId, instances)
        val deletedInstanceCount = withContext(Dispatchers.Loom) {
            instances.count { displayInstanceStore.delete(it.id) != null }
        }
        val deletedImageData = withContext(Dispatchers.Loom) {
            imageDataStore.delete(imageId) != null
        }
        val deletedImage = withContext(Dispatchers.Loom) {
            imageStore.delete(imageId) != null
        }

        sendMessage(
            "Delete finished. imageId=$imageId, imageName=${quote(image.name)}, deletedImage=$deletedImage, deletedImageData=$deletedImageData, " +
                    "displayInstancesFound=${instances.size}, displayInstancesDeleted=$deletedInstanceCount, runtimeDetached=${cleanupSummary.runtimeDetached}. "
        )
    }

    @Command("gallery debug get-item <id>")
    @Permission(PERMISSION_GALLERY_DEBUG)
    suspend fun CommandSender.getItem(@Argument("id") imageIdRaw: String) {
        val player = this as? Player
        if (player == null) {
            sendMessage("Get-item failed: this command must be executed by a player because it gives the image item to the executor.")
            return
        }

        val imageId = imageIdRaw.toUuidOrNull()
        if (imageId == null) {
            player.sendMessage("Get-item failed: id is not a valid UUID. id=${quote(imageIdRaw)}")
            return
        }

        val image = withContext(Dispatchers.Loom) {
            imageStore.get(imageId)
        }
        if (image == null) {
            player.sendMessage("Get-item failed: image does not exist. id=$imageId")
            return
        }

        val droppedItemCount = giveItem(player, createImageItem(image))
        player.sendMessage(
            "Get-item succeeded. imageId=${image.id}, type=${image.type}, name=${quote(image.name)}, owner=${image.ownerName}, size=${image.widthBlocks}x${image.heightBlocks}, droppedItemCount=$droppedItemCount"
        )
    }

    private suspend fun createImageFromUrl(
        owner: Player,
        name: String,
        url: String,
        width: Int,
        height: Int,
        mapCount: Int,
        renderComponents: RenderComponents,
    ): CreateImageDebugResult {
        val downloadedFile = runCatching { downloadFile(url) }
            .getOrElse {
                return CreateImageDebugResult.Failure(
                    "Create failed: unable to download url=${quote(url)}. cause=${it.message ?: it::class.qualifiedName}"
                )
            }

        val decodeResult = UnifiedImageDecoder.decode(
            bytes = downloadedFile.bytes,
            constraints = galleryConfig.decode.toConstraints(),
            fileNameHint = downloadedFile.fileNameHint,
        )

        val imageData = when (decodeResult) {
            is UnifiedImageDecoder.Result.Failure -> {
                return CreateImageDebugResult.Failure(
                    "Create failed: decoder returned ${describeDecodeResult(decodeResult.result)}. downloadedBytes=${downloadedFile.bytes.size}, fileNameHint=${
                        quote(
                            downloadedFile.fileNameHint
                        )
                    }"
                )
            }

            is UnifiedImageDecoder.Result.StaticSuccess -> {
                val decoded = decodeResult.result as DecodeResult.Success<PixelBuffer>
                when (val renderResult = StaticImageRenderer.render(
                    decoded.data,
                    buildBasicRenderSettings(renderComponents, width, height)
                )) {
                    is RenderResult.Success -> renderResult.data
                    else -> {
                        return CreateImageDebugResult.Failure(
                            "Create failed: static renderer returned ${describeRenderResult(renderResult)}. size=${width}x${height}"
                        )
                    }
                }
            }

            is UnifiedImageDecoder.Result.AnimatedSuccess -> {
                val decoded = decodeResult.result as DecodeResult.Success<AnimatedImageSource>
                when (
                    val renderResult = AnimatedImageRenderer.render(
                        source = decoded.data,
                        settings = buildAnimatedRenderSettings(renderComponents, width, height),
                    )
                ) {
                    is RenderResult.Success -> renderResult.data
                    else -> {
                        return CreateImageDebugResult.Failure(
                            "Create failed: animated renderer returned ${describeRenderResult(renderResult)}. size=${width}x${height}"
                        )
                    }
                }
            }
        }

        val allocatedMapIds = when (val allocationResult = allocateMapIdUseCase.execute(mapCount)) {
            is AllocateMapIdUseCase.Result.Success -> allocationResult.ids
            is AllocateMapIdUseCase.Result.IdRangeOverflow -> {
                return CreateImageDebugResult.Failure(
                    "Create failed: AllocateMapIdUseCase returned IdRangeOverflow. requestedMapCount=$mapCount, range=${allocationResult.range.start}..${allocationResult.range.end}"
                )
            }
        }

        val image = Image(
            id = UUID.randomUUID(),
            type = imageData.type,
            owner = owner.uniqueId,
            ownerName = owner.name,
            name = name,
            widthBlocks = width,
            heightBlocks = height,
            tileMapIds = allocatedMapIds,
        )

        val imageCreated = imageStore.create(image)
        if (!imageCreated) {
            return CreateImageDebugResult.Failure(
                "Create failed: ImageStore.create(image) returned false for generated imageId=${image.id}."
            )
        }

        val imageDataCreated = imageDataStore.create(image.id, imageData)
        if (!imageDataCreated) {
            val rollbackDeletedImage = imageStore.delete(image.id) != null
            return CreateImageDebugResult.Failure(
                "Create failed: ImageDataStore.create(image.id, imageData) returned false for imageId=${image.id}. rollbackDeletedImage=$rollbackDeletedImage"
            )
        }

        return CreateImageDebugResult.Success(
            image = image,
            imageData = imageData,
            downloadedBytes = downloadedFile.bytes.size,
        )
    }

    private fun cleanupDisplayInstances(
        imageId: UUID,
        instances: List<plutoproject.feature.gallery.core.display.DisplayInstance>
    ): DisplayCleanupSummary {
        var runtimeDetached = 0

        for (instance in instances) {
            if (displayRuntime.detach(imageId, instance.id) != null) {
                runtimeDetached++
            }
        }

        return DisplayCleanupSummary(
            runtimeDetached = runtimeDetached,
        )
    }

    private fun buildBasicRenderSettings(
        renderComponents: RenderComponents,
        width: Int,
        height: Int,
    ): BasicRenderSettings {
        return BasicRenderSettings(
            renderComponents = renderComponents,
            widthBlocks = width,
            heightBlocks = height,
            backgroundColor = DEFAULT_BACKGROUND_RGB24,
        )
    }

    private fun buildAnimatedRenderSettings(
        renderComponents: RenderComponents,
        width: Int,
        height: Int,
    ): AnimatedImageRenderSettings {
        val frameInterval = frameInterval(galleryConfig.display.animated.maxFramesPerSecond)
        return AnimatedImageRenderSettings(
            basicSettings = buildBasicRenderSettings(renderComponents, width, height),
            minFrameDuration = frameInterval,
            outputFrameInterval = frameInterval,
        )
    }

    private fun frameInterval(maxFramesPerSecond: Int): Duration {
        if (maxFramesPerSecond <= 0) {
            return 50.milliseconds
        }

        return maxOf(1L, ceil(1000.0 / maxFramesPerSecond.toDouble()).toLong()).milliseconds
    }

    private fun giveItem(player: Player, itemStack: ItemStack): Int {
        val leftovers = player.inventory.addItem(itemStack)
        leftovers.values.forEach { player.world.dropItemNaturally(player.location, it) }
        return leftovers.values.sumOf(ItemStack::getAmount)
    }

    private fun downloadFile(url: String): DownloadedFile {
        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} ${response.message}")
            }

            val body = response.body
            return DownloadedFile(
                bytes = body.bytes(),
                fileNameHint = response.request.url.pathSegments.lastOrNull()?.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun DecodeSettings.toConstraints(): DecodeConstraints {
        return DecodeConstraints(
            maxBytes = maxBytes,
            maxPixels = maxPixels,
            maxFrames = maxFrames,
        )
    }

    private fun describeDecodeResult(result: DecodeResult<*>): String {
        return when (result) {
            is DecodeResult.Success<*> -> "DecodeResult.Success(data=${result.data::class.simpleName})"
            DecodeResult.InvalidImage -> "DecodeResult.InvalidImage"
            DecodeResult.UnsupportedFormat -> "DecodeResult.UnsupportedFormat"
            DecodeResult.ImageTooLarge -> "DecodeResult.ImageTooLarge"
            DecodeResult.TooManyFrames -> "DecodeResult.TooManyFrames"
            is DecodeResult.UnknownFailure -> "DecodeResult.UnknownFailure(cause=${result.cause?.message ?: result.cause?.javaClass?.name ?: "null"})"
        }
    }

    private fun describeRenderResult(result: RenderResult<*>): String {
        return when (result) {
            is RenderResult.Success<*> -> "RenderResult.Success(data=${result.data::class.simpleName})"
            RenderResult.TilePoolOverflow -> "RenderResult.TilePoolOverflow"
            RenderResult.TileIndexCountOverflow -> "RenderResult.TileIndexCountOverflow"
            RenderResult.DurationOverflow -> "RenderResult.DurationOverflow"
            RenderResult.OutputFrameCountOverflow -> "RenderResult.OutputFrameCountOverflow"
        }
    }

    private fun String.toUuidOrNull(): UUID? {
        return runCatching { UUID.fromString(this) }.getOrNull()
    }

    private fun quote(value: String?): String {
        return if (value == null) "null" else "\"$value\""
    }
}

private sealed interface CreateImageDebugResult {
    data class Success(
        val image: Image,
        val imageData: ImageData,
        val downloadedBytes: Int,
    ) : CreateImageDebugResult

    data class Failure(val message: String) : CreateImageDebugResult
}

private data class DownloadedFile(
    val bytes: ByteArray,
    val fileNameHint: String?,
)

private data class DisplayCleanupSummary(
    val runtimeDetached: Int,
)
