package plutoproject.feature.gallery.adapter.paper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.feature.gallery.adapter.common.DecodeSettings
import plutoproject.feature.gallery.adapter.common.DitherMode
import plutoproject.feature.gallery.adapter.common.GalleryConfig
import plutoproject.feature.gallery.adapter.common.QuantizeMode
import plutoproject.feature.gallery.adapter.common.RepositionMode
import plutoproject.feature.gallery.adapter.common.ScaleMode
import plutoproject.feature.gallery.adapter.common.UploadService
import plutoproject.feature.gallery.adapter.common.UploadSession
import plutoproject.feature.gallery.adapter.common.UploadState
import plutoproject.feature.gallery.adapter.common.VerificationResult
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.core.AllocateMapIdUseCase
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.UnifiedImageDecoder
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.feature.gallery.core.render.AnimatedImageRenderSettings
import plutoproject.feature.gallery.core.render.AnimatedImageRenderer
import plutoproject.feature.gallery.core.render.BasicRenderSettings
import plutoproject.feature.gallery.core.render.PixelBuffer
import plutoproject.feature.gallery.core.render.RenderComponents
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.StaticImageRenderer
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.paper.util.coroutine.coroutineContext
import java.util.UUID
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
    private val uploadService = koin.get<UploadService>()
    private val coroutineScope = koin.get<CoroutineScope>()

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

        val request = validateCreateRequest(
            failurePrefix = "Create failed",
            player = player,
            name = name,
            width = width,
            height = height,
            repositionMode = repositionMode,
            scaleMode = scaleMode,
            quantizeMode = quantizeMode,
            ditherMode = ditherMode,
        ) ?: return

        val createResult = withContext(Dispatchers.Loom) {
            createImageFromUrl(
                ownerId = player.uniqueId,
                ownerName = player.name,
                url = url,
                request = request,
            )
        }

        when (createResult) {
            is CreateImageDebugResult.Failure -> player.sendMessage(createResult.message)
            is CreateImageDebugResult.Success -> sendCreateSuccessMessage(
                playerId = player.uniqueId,
                prefix = "Create succeeded",
                result = createResult,
                request = request,
                sourceDescription = "url=${quote(url)}",
            )
        }
    }

    @Command("gallery debug create-upload <name> <width> <height> [repositionMode] [scaleMode] [quantizeMode] [ditherMode]")
    @Permission(PERMISSION_GALLERY_DEBUG)
    suspend fun CommandSender.createUpload(
        @Argument("name") @Quoted name: String,
        @Argument("width") width: Int,
        @Argument("height") height: Int,
        @Argument("repositionMode") repositionMode: RepositionMode?,
        @Argument("scaleMode") scaleMode: ScaleMode?,
        @Argument("quantizeMode") quantizeMode: QuantizeMode?,
        @Argument("ditherMode") ditherMode: DitherMode?,
    ) {
        val player = this as? Player
        if (player == null) {
            sendMessage("Create-upload failed: this command must be executed by a player because it creates an upload session and later gives the created image item to the executor.")
            return
        }

        val request = validateCreateRequest(
            failurePrefix = "Create-upload failed",
            player = player,
            name = name,
            width = width,
            height = height,
            repositionMode = repositionMode,
            scaleMode = scaleMode,
            quantizeMode = quantizeMode,
            ditherMode = ditherMode,
        ) ?: return

        val session = uploadService.createUploadSession(player.uniqueId)
        player.sendMessage(
            "Create-upload session created. sessionId=${session.id}, uploadUrl=${quote(session.uploadUrl)}, name=${quote(request.name)}, " +
                "size=${request.width}x${request.height}, mapCount=${request.mapCount}, modes={reposition=${request.repositionMode}, scale=${request.scaleMode}, quantize=${request.quantizeMode}, dither=${request.ditherMode}}"
        )

        coroutineScope.launch {
            monitorUploadSession(
                playerId = player.uniqueId,
                ownerId = player.uniqueId,
                ownerName = player.name,
                session = session,
                request = request,
            )
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
                "displayInstancesFound=${instances.size}, displayInstancesDeleted=$deletedInstanceCount, runtimeDetached=${cleanupSummary.runtimeDetached}. " +
                "Chunk-related cleanup is intentionally skipped here: chunk index and item frame contents are left for the real event-driven cleanup path."
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
        ownerId: UUID,
        ownerName: String,
        url: String,
        request: CreateRequest,
    ): CreateImageDebugResult {
        val downloadedFile = runCatching { downloadFile(url) }
            .getOrElse {
                return CreateImageDebugResult.Failure(
                    "Create failed: unable to download url=${quote(url)}. cause=${it.message ?: it::class.qualifiedName}"
                )
            }

        return createImageFromBytes(
            ownerId = ownerId,
            ownerName = ownerName,
            request = request,
            bytes = downloadedFile.bytes,
            fileNameHint = downloadedFile.fileNameHint,
        )
    }

    private suspend fun createImageFromBytes(
        ownerId: UUID,
        ownerName: String,
        request: CreateRequest,
        bytes: ByteArray,
        fileNameHint: String?,
    ): CreateImageDebugResult {
        val decodeResult = UnifiedImageDecoder.decode(
            bytes = bytes,
            constraints = galleryConfig.decode.toConstraints(),
            fileNameHint = fileNameHint,
        )

        val imageData = when (decodeResult) {
            is UnifiedImageDecoder.Result.Failure -> {
                return CreateImageDebugResult.Failure(
                    "Create failed: decoder returned ${describeDecodeResult(decodeResult.result)}. sourceBytes=${bytes.size}, fileNameHint=${quote(fileNameHint)}"
                )
            }

            is UnifiedImageDecoder.Result.StaticSuccess -> {
                val decoded = decodeResult.result as DecodeResult.Success<PixelBuffer>
                when (
                    val renderResult = StaticImageRenderer.render(
                        decoded.data,
                        buildBasicRenderSettings(request.renderComponents, request.width, request.height),
                    )
                ) {
                    is RenderResult.Success -> renderResult.data
                    else -> {
                        return CreateImageDebugResult.Failure(
                            "Create failed: static renderer returned ${describeRenderResult(renderResult)}. size=${request.width}x${request.height}"
                        )
                    }
                }
            }

            is UnifiedImageDecoder.Result.AnimatedSuccess -> {
                val decoded = decodeResult.result as DecodeResult.Success<AnimatedImageSource>
                when (
                    val renderResult = AnimatedImageRenderer.render(
                        source = decoded.data,
                        settings = buildAnimatedRenderSettings(request.renderComponents, request.width, request.height),
                    )
                ) {
                    is RenderResult.Success -> renderResult.data
                    else -> {
                        return CreateImageDebugResult.Failure(
                            "Create failed: animated renderer returned ${describeRenderResult(renderResult)}. size=${request.width}x${request.height}"
                        )
                    }
                }
            }
        }

        val allocatedMapIds = when (val allocationResult = allocateMapIdUseCase.execute(request.mapCount)) {
            is AllocateMapIdUseCase.Result.Success -> allocationResult.ids
            is AllocateMapIdUseCase.Result.IdRangeOverflow -> {
                return CreateImageDebugResult.Failure(
                    "Create failed: AllocateMapIdUseCase returned IdRangeOverflow. requestedMapCount=${request.mapCount}, range=${allocationResult.range.start}..${allocationResult.range.end}"
                )
            }
        }

        val image = Image(
            id = UUID.randomUUID(),
            type = imageData.type,
            owner = ownerId,
            ownerName = ownerName,
            name = request.name,
            widthBlocks = request.width,
            heightBlocks = request.height,
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
            sourceBytes = bytes.size,
        )
    }

    private fun validateCreateRequest(
        failurePrefix: String,
        player: Player,
        name: String,
        width: Int,
        height: Int,
        repositionMode: RepositionMode?,
        scaleMode: ScaleMode?,
        quantizeMode: QuantizeMode?,
        ditherMode: DitherMode?,
    ): CreateRequest? {
        if (name.isBlank()) {
            player.sendMessage("$failurePrefix: image name must not be blank.")
            return null
        }
        if (width <= 0 || height <= 0) {
            player.sendMessage("$failurePrefix: width and height must both be > 0. width=$width, height=$height")
            return null
        }

        val mapCount = runCatching { Math.multiplyExact(width, height) }
            .getOrElse {
                player.sendMessage("$failurePrefix: width * height overflowed Int. width=$width, height=$height")
                return null
            }

        val effectiveRepositionMode = repositionMode ?: galleryConfig.render.defaultRepositionMode
        val effectiveScaleMode = scaleMode ?: galleryConfig.render.defaultScaleMode
        val effectiveQuantizeMode = quantizeMode ?: galleryConfig.render.defaultQuantizeMode
        val effectiveDitherMode = ditherMode ?: galleryConfig.render.defaultDitherMode

        return CreateRequest(
            name = name,
            width = width,
            height = height,
            mapCount = mapCount,
            repositionMode = effectiveRepositionMode,
            scaleMode = effectiveScaleMode,
            quantizeMode = effectiveQuantizeMode,
            ditherMode = effectiveDitherMode,
            renderComponents = RenderComponents(
                repositioner = effectiveRepositionMode.repositioner,
                scaler = effectiveScaleMode.scaler,
                quantizer = effectiveQuantizeMode.quantizer,
                ditherer = effectiveDitherMode.ditherer,
            ),
        )
    }

    private suspend fun monitorUploadSession(
        playerId: UUID,
        ownerId: UUID,
        ownerName: String,
        session: UploadSession,
        request: CreateRequest,
    ) {
        session.state.drop(1).first { state ->
            when (state) {
                UploadState.Waiting -> {
                    sendPlayerMessage(
                        playerId,
                        "Create-upload session update: state=Waiting, sessionId=${session.id}. The session can accept another upload attempt before expiration. uploadUrl=${quote(session.uploadUrl)}"
                    )
                    false
                }

                UploadState.Processing -> {
                    sendPlayerMessage(
                        playerId,
                        "Create-upload session update: state=Processing, sessionId=${session.id}. The uploaded file is being verified."
                    )
                    false
                }

                UploadState.Expired -> {
                    sendPlayerMessage(
                        playerId,
                        "Create-upload session update: state=Expired, sessionId=${session.id}. The upload link can no longer be used."
                    )
                    true
                }

                is UploadState.VerificationFailure -> {
                    sendPlayerMessage(
                        playerId,
                        "Create-upload session update: state=VerificationFailure, sessionId=${session.id}. reason=${describeVerificationResult(state.result)}"
                    )
                    true
                }

                is UploadState.Success -> {
                    sendPlayerMessage(
                        playerId,
                        "Create-upload session update: state=Success, sessionId=${session.id}. The file upload succeeded and image creation is starting."
                    )

                    val uploadedBytes = withContext(Dispatchers.Loom) {
                        state.uploadedFile.use { inputStream -> inputStream.readBytes() }
                    }
                    val bytes = uploadedBytes.getOrElse {
                        sendPlayerMessage(
                            playerId,
                            "Create-upload failed after upload success: unable to read uploaded temp file for sessionId=${session.id}. cause=${it.message ?: it::class.qualifiedName}"
                        )
                        return@first true
                    }

                    val createResult = withContext(Dispatchers.Loom) {
                        createImageFromBytes(
                            ownerId = ownerId,
                            ownerName = ownerName,
                            request = request,
                            bytes = bytes,
                            fileNameHint = null,
                        )
                    }

                    when (createResult) {
                        is CreateImageDebugResult.Failure -> {
                            sendPlayerMessage(
                                playerId,
                                "Create-upload failed after upload success. sessionId=${session.id}. ${createResult.message}"
                            )
                        }

                        is CreateImageDebugResult.Success -> {
                            sendCreateSuccessMessage(
                                playerId = playerId,
                                prefix = "Create-upload succeeded",
                                result = createResult,
                                request = request,
                                sourceDescription = "uploadSessionId=${session.id}, uploadUrl=${quote(session.uploadUrl)}",
                            )
                        }
                    }
                    true
                }
            }
        }
    }

    private suspend fun sendPlayerMessage(playerId: UUID, message: String) {
        withContext(Bukkit.getServer().coroutineContext) {
            Bukkit.getPlayer(playerId)?.sendMessage(message)
        }
    }

    private suspend fun sendCreateSuccessMessage(
        playerId: UUID,
        prefix: String,
        result: CreateImageDebugResult.Success,
        request: CreateRequest,
        sourceDescription: String,
    ) {
        withContext(Bukkit.getServer().coroutineContext) {
            val player = Bukkit.getPlayer(playerId) ?: return@withContext
            val droppedItemCount = giveItem(player, createImageItem(result.image))
            player.sendMessage(
                "$prefix. imageId=${result.image.id}, type=${result.image.type}, name=${quote(result.image.name)}, $sourceDescription, " +
                    "sourceBytes=${result.sourceBytes}, size=${request.width}x${request.height}, mapCount=${request.mapCount}, uniqueTileCount=${result.imageData.tilePool.tileCount}, " +
                    "modes={reposition=${request.repositionMode}, scale=${request.scaleMode}, quantize=${request.quantizeMode}, dither=${request.ditherMode}}, droppedItemCount=$droppedItemCount"
            )
        }
    }

    private fun cleanupDisplayInstances(
        imageId: UUID,
        instances: List<DisplayInstance>,
    ): DisplayCleanupSummary {
        var runtimeDetached = 0

        for (instance in instances) {
            if (displayRuntime.detach(imageId, instance.id) != null) {
                runtimeDetached++
            }
        }

        return DisplayCleanupSummary(runtimeDetached = runtimeDetached)
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

    private fun describeVerificationResult(result: VerificationResult): String {
        return when (result) {
            VerificationResult.Pass -> "VerificationResult.Pass"
            is VerificationResult.FileTooLarge -> "VerificationResult.FileTooLarge(size=${result.size})"
            is VerificationResult.ImageTooLarge -> "VerificationResult.ImageTooLarge(width=${result.width}, height=${result.height}, pixels=${result.pixels})"
            is VerificationResult.ImageTooSmall -> "VerificationResult.ImageTooSmall(width=${result.width}, height=${result.height}, pixels=${result.pixels})"
            is VerificationResult.AbnormalAspectRatio -> "VerificationResult.AbnormalAspectRatio(width=${result.width}, height=${result.height}, aspectRatio=${result.aspectRatio})"
            is VerificationResult.UnallowedExtension -> "VerificationResult.UnallowedExtension(fileName=${quote(result.fileName)})"
            VerificationResult.UnsupportedFormat -> "VerificationResult.UnsupportedFormat"
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
        val sourceBytes: Int,
    ) : CreateImageDebugResult

    data class Failure(val message: String) : CreateImageDebugResult
}

private data class CreateRequest(
    val name: String,
    val width: Int,
    val height: Int,
    val mapCount: Int,
    val repositionMode: RepositionMode,
    val scaleMode: ScaleMode,
    val quantizeMode: QuantizeMode,
    val ditherMode: DitherMode,
    val renderComponents: RenderComponents,
)

private data class DownloadedFile(
    val bytes: ByteArray,
    val fileNameHint: String?,
)

private data class DisplayCleanupSummary(
    val runtimeDetached: Int,
)
