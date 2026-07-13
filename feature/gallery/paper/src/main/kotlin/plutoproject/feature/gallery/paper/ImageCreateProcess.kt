package plutoproject.feature.gallery.paper

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import ink.pmc.advkt.component.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import plutoproject.feature.gallery.common.*
import plutoproject.feature.gallery.common.upload.*
import plutoproject.feature.gallery.core.AllocateMapIdUseCase
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.UnifiedImageDecoder
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataStore
import plutoproject.feature.gallery.core.image.ImageStore
import plutoproject.feature.gallery.core.render.*
import plutoproject.foundation.common.text.MESSAGE_SOUND
import plutoproject.foundation.common.text.UI_FAILED_SOUND
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.time.toFormattedComponent
import plutoproject.kernel.api.koinGet
import plutoproject.kernel.api.koinInject
import java.nio.file.Path
import java.util.*

private const val DEFAULT_BACKGROUND_RGB24 = 0x000000

private val config by koinInject<GalleryConfig>()
private val allocateMapIdUseCase by koinInject<AllocateMapIdUseCase>()
private val imageStore by koinInject<ImageStore>()
private val imageDataStore by koinInject<ImageDataStore>()
private val displayInstanceStore by koinInject<DisplayInstanceStore>()
private val displayRuntime by koinInject<DisplayRuntimeRegistry>()
private val uploadService by koinInject<UploadService>()
private val coroutineScope by koinInject<CoroutineScope>()
private val serverContext by lazy { koinGet<Plugin>().minecraftDispatcher }

sealed interface ImageCreateSubmissionResult {
    data class Created(val session: UploadSession) : ImageCreateSubmissionResult
    data object TooManyMapBlocks : ImageCreateSubmissionResult
    data object TooManyImages : ImageCreateSubmissionResult
    data class UnfinishedSessionExists(val session: UploadSession) : ImageCreateSubmissionResult
}

private data class ImageCreateRequest(
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

private sealed interface ImageCreateResult {
    data class Success(val image: Image) : ImageCreateResult

    data class Failure(val message: Component) : ImageCreateResult
}

suspend fun hasUnfinishedImageCreateSession(playerId: UUID): Boolean {
    return uploadService.getUnfinishedSession(playerId) != null
}

suspend fun hasReachedImageLimit(playerId: UUID): Boolean {
    return imageStore.findByOwner(playerId).size >= config.image.maxImagesPerPlayer
}

suspend fun deleteOwnedImage(playerId: UUID, imageId: UUID): Boolean {
    return withContext(Dispatchers.IO) {
        val image = imageStore.get(imageId) ?: return@withContext false
        if (image.owner != playerId) {
            return@withContext false
        }

        displayRuntime.stop(imageId)
        displayInstanceStore.findByImageId(imageId).forEach {
            displayInstanceStore.delete(it.id)
        }
        imageDataStore.delete(imageId)
        imageStore.delete(imageId) != null
    }
}

suspend fun submitImageCreate(
    player: Player,
    name: String,
    width: Int,
    height: Int,
    repositionMode: RepositionMode,
    ditherMode: DitherMode,
): ImageCreateSubmissionResult {
    val mapCount = runCatching { Math.multiplyExact(width, height) }
        .getOrElse { return ImageCreateSubmissionResult.TooManyMapBlocks }
    if (mapCount > config.image.maxMapBlocks) {
        return ImageCreateSubmissionResult.TooManyMapBlocks
    }
    if (hasReachedImageLimit(player.uniqueId)) {
        return ImageCreateSubmissionResult.TooManyImages
    }

    val request = ImageCreateRequest(
        name = name,
        width = width,
        height = height,
        mapCount = mapCount,
        repositionMode = repositionMode,
        scaleMode = config.render.scaleMode,
        quantizeMode = config.render.quantizeMode,
        ditherMode = ditherMode,
        renderComponents = RenderComponents(
            repositioner = repositionMode.repositioner,
            scaler = config.render.scaleMode.scaler,
            quantizer = config.render.quantizeMode.quantizer,
            ditherer = ditherMode.ditherer,
        ),
    )

    return when (val result = uploadService.createSessionIfAbsent(player.uniqueId)) {
        is UploadSessionCreateResult.Conflict -> {
            ImageCreateSubmissionResult.UnfinishedSessionExists(result.session)
        }

        is UploadSessionCreateResult.Created -> {
            player.sendMessage(
                component {
                    raw(IMAGE_CREATE_SESSION_CREATED_PREFIX)
                    newline()
                    text("[点击打开]") with mochaLavender with underlined() with openUrl(result.session.uploadUrl) with showText {
                        text("点击打开上传页面") with mochaLavender
                    }
                    text(" ")
                    raw(IMAGE_CREATE_SESSION_CREATED_CANCEL)
                    newline()
                    raw(
                        IMAGE_CREATE_SESSION_CREATED_SUFFIX.replace(
                            IMAGE_PLACEHOLDER_EXPIRE,
                            config.upload.requestExpireAfter.toFormattedComponent()
                        )
                    )
                }
            )
            player.playSound(MESSAGE_SOUND)

            coroutineScope.launch {
                monitorUploadSession(
                    playerId = player.uniqueId,
                    ownerId = player.uniqueId,
                    ownerName = player.name,
                    session = result.session,
                    request = request,
                )
            }

            ImageCreateSubmissionResult.Created(result.session)
        }
    }
}

private suspend fun monitorUploadSession(
    playerId: UUID,
    ownerId: UUID,
    ownerName: String,
    session: UploadSession,
    request: ImageCreateRequest,
) {
    session.state.first { state ->
        handleUploadSessionState(
            playerId = playerId,
            ownerId = ownerId,
            ownerName = ownerName,
            request = request,
            state = state,
        )
    }
}

private suspend fun handleUploadSessionState(
    playerId: UUID,
    ownerId: UUID,
    ownerName: String,
    request: ImageCreateRequest,
    state: UploadState,
): Boolean {
    return when (state) {
        UploadState.Waiting -> false

        UploadState.Processing -> {
            sendPlayerMessage(playerId, IMAGE_CREATE_SESSION_PROCESSING, MESSAGE_SOUND)
            false
        }

        UploadState.Expired -> {
            sendPlayerMessage(playerId, IMAGE_CREATE_SESSION_EXPIRED, UI_FAILED_SOUND)
            true
        }

        is UploadState.VerificationFailed -> {
            sendPlayerMessage(playerId, getImageCreateVerificationFailedMessage(state.result), UI_FAILED_SOUND)
            true
        }

        is UploadState.Completed -> {
            sendPlayerMessage(playerId, IMAGE_CREATE_SESSION_CREATING, MESSAGE_SOUND)

            if (!isPlayerOnline(playerId)) {
                return true
            }

            val createResult = withContext(Dispatchers.IO) {
                state.file.usePath { tempFile ->
                    createImageFromPath(
                        ownerId = ownerId,
                        ownerName = ownerName,
                        request = request,
                        path = tempFile,
                    )
                }
            }
            val result = createResult.getOrElse {
                sendPlayerMessage(playerId, IMAGE_CREATE_FAILED_SERVER, UI_FAILED_SOUND)
                return true
            }

            when (result) {
                is ImageCreateResult.Failure -> {
                    sendPlayerMessage(playerId, result.message, UI_FAILED_SOUND)
                }

                is ImageCreateResult.Success -> {
                    if (!isPlayerOnline(playerId)) {
                        deleteCreatedImage(result.image.id)
                        return true
                    }

                    sendCreateSuccessMessage(playerId, result.image)
                }
            }
            true
        }

        UploadState.Cancelled -> {
            sendPlayerMessage(playerId, IMAGE_CREATE_SESSION_CANCELLED, MESSAGE_SOUND)
            true
        }

        is UploadState.Failed -> {
            sendPlayerMessage(playerId, IMAGE_CREATE_SESSION_FAILED, UI_FAILED_SOUND)
            true
        }
    }
}

private suspend fun createImageFromPath(
    ownerId: UUID,
    ownerName: String,
    request: ImageCreateRequest,
    path: Path,
): ImageCreateResult {
    val decodeResult = UnifiedImageDecoder.decode(path, config.fileProcessing.decodeConstraints())

    val imageData = when (decodeResult) {
        is UnifiedImageDecoder.Result.Failure -> {
            return ImageCreateResult.Failure(IMAGE_CREATE_FAILED_INVALID_IMAGE)
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
                else -> return ImageCreateResult.Failure(IMAGE_CREATE_FAILED_SERVER)
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
                else -> return ImageCreateResult.Failure(IMAGE_CREATE_FAILED_SERVER)
            }
        }
    }

    val allocatedMapIds = when (val allocationResult = allocateMapIdUseCase.execute(request.mapCount)) {
        is AllocateMapIdUseCase.Result.Success -> allocationResult.ids
        is AllocateMapIdUseCase.Result.IdRangeOverflow -> return ImageCreateResult.Failure(IMAGE_CREATE_FAILED_SERVER)
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

    if (!imageStore.create(image)) {
        return ImageCreateResult.Failure(IMAGE_CREATE_FAILED_SERVER)
    }

    if (!imageDataStore.create(image.id, imageData)) {
        imageStore.delete(image.id)
        return ImageCreateResult.Failure(IMAGE_CREATE_FAILED_SERVER)
    }

    return ImageCreateResult.Success(
        image = image,
    )
}

private suspend fun sendCreateSuccessMessage(playerId: UUID, image: Image) {
    withContext(serverContext) {
        val player = Bukkit.getPlayer(playerId) ?: return@withContext
        giveItem(player, createImageItem(image))
        player.sendMessage(IMAGE_CREATE_SUCCEEDED.replace(IMAGE_PLACEHOLDER_NAME, image.name))
        player.playSound(UI_SUCCEED_SOUND)
    }
}

private suspend fun isPlayerOnline(playerId: UUID): Boolean {
    return withContext(serverContext) {
        val player = Bukkit.getPlayer(playerId) ?: return@withContext false
        player.isOnline && player.isConnected
    }
}

private suspend fun deleteCreatedImage(imageId: UUID) {
    withContext(Dispatchers.IO) {
        imageDataStore.delete(imageId)
        imageStore.delete(imageId)
    }
}

private suspend fun sendPlayerMessage(playerId: UUID, message: Component, sound: Sound? = null) {
    withContext(serverContext) {
        val player = Bukkit.getPlayer(playerId) ?: return@withContext
        player.sendMessage(message)
        if (sound != null) {
            player.playSound(sound)
        }
    }
}

private fun getImageCreateVerificationFailedMessage(result: VerificationResult.Rejected): Component {
    val reason = when (result) {
        is VerificationResult.FileTooLarge -> IMAGE_CREATE_VERIFICATION_FAILED_FILE_TOO_LARGE
        is VerificationResult.ImageTooLarge -> IMAGE_CREATE_VERIFICATION_FAILED_IMAGE_TOO_LARGE
            .replace(IMAGE_PLACEHOLDER_IMAGE_WIDTH, result.width)
            .replace(IMAGE_PLACEHOLDER_IMAGE_HEIGHT, result.height)

        is VerificationResult.TooManyFrames -> IMAGE_CREATE_VERIFICATION_FAILED_TOO_MANY_FRAMES
            .replace(IMAGE_PLACEHOLDER_FRAME_COUNT, result.frameCount)

        is VerificationResult.UnallowedExtension -> IMAGE_CREATE_VERIFICATION_FAILED_UNALLOWED_EXTENSION
            .replace(IMAGE_PLACEHOLDER_FILE_NAME, result.fileName)

        VerificationResult.UnsupportedFormat -> IMAGE_CREATE_VERIFICATION_FAILED_UNSUPPORTED_FORMAT
        VerificationResult.Corrupted -> IMAGE_CREATE_VERIFICATION_FAILED_CORRUPTED
        is VerificationResult.Failed -> IMAGE_CREATE_VERIFICATION_FAILED_UNKNOWN
    }

    return component {
        raw(IMAGE_CREATE_VERIFICATION_FAILED_PREFIX)
        raw(reason)
    }
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
    return AnimatedImageRenderSettings(
        basicSettings = buildBasicRenderSettings(renderComponents, width, height),
        minFrameDuration = config.render.animated.minFrameDuration,
        outputFrameInterval = config.render.animated.outputFrameInterval,
    )
}

private fun giveItem(player: Player, itemStack: ItemStack): Int {
    val leftovers = player.inventory.addItem(itemStack)
    leftovers.values.forEach { player.world.dropItemNaturally(player.location, it) }
    return leftovers.values.sumOf(ItemStack::getAmount)
}

private fun FileProcessingSettings.decodeConstraints(): DecodeConstraints {
    return DecodeConstraints(
        maxBytes = maxBytes,
        maxPixels = maxPixels,
        maxFrames = maxFrames,
    )
}
