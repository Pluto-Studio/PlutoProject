package plutoproject.feature.gallery.adapter.common

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import plutoproject.feature.gallery.core.decode.DecodableImageFormat
import plutoproject.feature.gallery.core.decode.ImageFormatSniffer
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlin.math.max
import kotlin.math.min

private val config by lazy { koin.get<GalleryConfig>() }
private val logger by lazy { koin.get<Logger>() }

private const val TEMP_FOLDER_SUB = "plutoproject_gallery_"

fun initializeTempFolder(): Result<Path> {
    cleanupTempFolders()

    val sub = "$TEMP_FOLDER_SUB${UUID.randomUUID()}"
    val path = Path(config.upload.tempFolderRoot, sub)

    return runCatching {
        path.createDirectories()
        path
    }.onFailure { exception ->
        logger.log(Level.SEVERE, "An error occurred while creating temp folder", exception)
    }
}

@OptIn(ExperimentalPathApi::class)
private fun cleanupTempFolders() {
    val tempRoot = Path(config.upload.tempFolderRoot)
    runCatching {
        if (!tempRoot.exists()) return@runCatching
        tempRoot.listDirectoryEntries()
            .filter { it.fileName.toString().startsWith(TEMP_FOLDER_SUB) }
            .forEach { it.deleteRecursively() }
    }.onFailure {
        logger.log(Level.WARNING, "An error occurred while cleaning-up temp folders", it)
    }
}

sealed interface VerificationResult {
    data object Pass : VerificationResult
    data class FileTooLarge(val size: Int) : VerificationResult
    data class ImageTooLarge(val width: Int, val height: Int, val pixels: Int) : VerificationResult
    data class ImageTooSmall(val width: Int, val height: Int, val pixels: Int) : VerificationResult
    data class AbnormalAspectRatio(val width: Int, val height: Int, val aspectRatio: Double) : VerificationResult
    data class UnallowedExtension(val fileName: String) : VerificationResult
    data object UnsupportedFormat : VerificationResult
}

sealed interface UploadState {
    data class Success(val uploadedFile: UploadedFile) : UploadState
    data object Waiting : UploadState
    data object Expired : UploadState
    data object Processing : UploadState
    data object Cancelled : UploadState
    data class UnknownFailure(val cause: Throwable?) : UploadState
    data class VerificationFailure(val result: VerificationResult) : UploadState
}

sealed interface UploadSubmissionResult {
    data object Accepted : UploadSubmissionResult
    data object NotFound : UploadSubmissionResult
    data object Expired : UploadSubmissionResult
    data object Conflict : UploadSubmissionResult
    data class UnknownFailure(val cause: Throwable) : UploadSubmissionResult
}

class UploadSession(
    val id: UUID,
    val creator: UUID,
    val createdAt: Instant,
    val state: MutableStateFlow<UploadState>,
    val uploadUrl: String,
    val expiryJob: Job
)

/**
 * 代表一个被上传的临时图像文件，没有线程安全保障。
 */
class UploadedFile(private val tempFile: Path) {
    init {
        check(tempFile.exists()) { "Temp file must be existed" }
        check(tempFile.isReadable()) { "Temp file must be readable" }
    }

    var isDiscarded = false
        private set
    private var inputStream: InputStream? = null

    fun <T> use(block: (InputStream) -> T): Result<T> {
        val inputStream = inputStream().getOrElse { return Result.failure(it) }
        return try {
            Result.success(block(inputStream))
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "An error occurred while using uploaded file", e)
            Result.failure(e)
        } finally {
            discard()
        }
    }

    private fun inputStream(): Result<InputStream> = runCatching {
        BufferedInputStream(tempFile.inputStream()).also { inputStream = it }
    }.onFailure { exception ->
        logger.log(Level.SEVERE, "An error occurred while opening input stream for temp file", exception)
    }

    fun discard() {
        if (isDiscarded) {
            return
        }

        isDiscarded = true

        listOf(
            runCatching { inputStream?.close() } to "closing input stream for temp file",
            runCatching { tempFile.deleteIfExists() } to "deleting temp file"
        ).forEach { (result, action) ->
            result.onFailure {
                logger.log(Level.SEVERE, "An error occurred while $action", it)
            }
        }
    }
}

class UploadService(
    private val clock: Clock,
    private val tempFolder: Path,
    private val coroutineScope: CoroutineScope,
) {
    init {
        check(tempFolder.exists()) { "Temp folder must be existed" }
        check(tempFolder.isReadable() && tempFolder.isWritable()) { "Temp folder must be readable and writable" }
    }

    @Volatile
    private var isClosed = false
    private val uploadSessionsById = ConcurrentHashMap<UUID, UploadSession>()

    private val uploadConfig by lazy { koin.get<GalleryConfig>().upload }
    private val allowedExtensions = uploadConfig.allowedFileExtensions.map { it.lowercase() }.toSet()
    private val supportedMimeTypes = listOf(
        ContentType.Image.PNG,
        ContentType.Image.JPEG,
        ContentType("image", "webp"),
        ContentType.Image.GIF
    )

    fun createSession(creator: UUID): UploadSession {
        check(!isClosed) { "UploadService is closed" }
        val id = UUID.randomUUID()
        return UploadSession(
            id = id,
            creator = creator,
            createdAt = clock.instant(),
            state = MutableStateFlow(UploadState.Waiting),
            uploadUrl = "${uploadConfig.baseUrl.trimEnd('/')}/upload/$id",
            expiryJob = createExpiryJob(id)
        ).also {
            uploadSessionsById[id] = it
        }
    }

    private fun createExpiryJob(sessionId: UUID): Job {
        return coroutineScope.launch {
            delay(uploadConfig.requestExpireAfter)
            tryExpire(sessionId)
        }
    }

    private fun tryExpire(sessionId: UUID) {
        val uploadSession = uploadSessionsById[sessionId] ?: return
        uploadSession.state.compareAndSet(UploadState.Waiting, UploadState.Expired)
    }

    fun findUploadSession(id: UUID): UploadSession? {
        return uploadSessionsById[id]
    }

    suspend fun submitUpload(
        id: UUID,
        fileName: String?,
        contentType: ContentType?,
        bytes: ByteArray,
    ): UploadSubmissionResult {
        check(!isClosed) { "UploadService is closed" }

        val session = uploadSessionsById[id] ?: return UploadSubmissionResult.NotFound
        if (!session.state.compareAndSet(UploadState.Waiting, UploadState.Processing)) {
            return when (session.state.value) {
                UploadState.Expired -> UploadSubmissionResult.Expired
                else -> UploadSubmissionResult.Conflict
            }
        }

        val verificationResult = verifyFile(fileName, contentType, bytes)
        if (verificationResult != VerificationResult.Pass) {
            updateUploadSession(id, UploadState.VerificationFailure(verificationResult))
            return UploadSubmissionResult.Accepted
        }

        return runCatching {
            val tempFile = createTempFileFor(id, fileName)
            tempFile.writeBytes(bytes)
            updateUploadSession(id, UploadState.Success(UploadedFile(tempFile)))
            UploadSubmissionResult.Accepted
        }.getOrElse { exception ->
            logger.log(Level.SEVERE, "An error occurred while storing uploaded file", exception)
            updateUploadSession(id, UploadState.UnknownFailure(exception))
            UploadSubmissionResult.UnknownFailure(exception)
        }
    }

    private fun updateUploadSession(sessionId: UUID, newState: UploadState) {
        val session = uploadSessionsById[sessionId] ?: return
        session.state.value = newState
    }

    @OptIn(ExperimentalPathApi::class)
    fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        uploadSessionsById.forEach { (_, session) ->
            session.expiryJob.cancel()
            if (session.state.value !is UploadState.Success) {
                session.state.value = UploadState.Cancelled
                return@forEach
            }
            val uploadedFile = (session.state.value as UploadState.Success).uploadedFile
            uploadedFile.discard()
        }
        uploadSessionsById.clear()
        tempFolder.deleteRecursively()
    }

    private suspend fun verifyFile(
        fileName: String?,
        contentType: ContentType?,
        bytes: ByteArray,
    ): VerificationResult {
        if (bytes.size > uploadConfig.maxBytes) {
            return VerificationResult.FileTooLarge(bytes.size)
        }

        val extension = fileName.extensionOrEmpty()
        if (extension !in allowedExtensions) {
            return VerificationResult.UnallowedExtension(fileName.orEmpty())
        }

        if (contentType != null && contentType !in supportedMimeTypes) {
            return VerificationResult.UnsupportedFormat
        }

        val format = ImageFormatSniffer.sniff(bytes, fileName) ?: return VerificationResult.UnsupportedFormat
        val dimensions = readImageDimensions(bytes, format) ?: return VerificationResult.UnsupportedFormat
        val pixels = (dimensions.width.toLong() * dimensions.height.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        if (
            dimensions.width > uploadConfig.maxWidth ||
            dimensions.height > uploadConfig.maxHeight ||
            pixels > uploadConfig.maxPixels
        ) {
            return VerificationResult.ImageTooLarge(dimensions.width, dimensions.height, pixels)
        }

        val shortEdge = min(dimensions.width, dimensions.height)
        if (shortEdge < uploadConfig.minShortEdge || pixels < uploadConfig.minPixels) {
            return VerificationResult.ImageTooSmall(dimensions.width, dimensions.height, pixels)
        }

        val aspectRatio = max(dimensions.width, dimensions.height).toDouble() / shortEdge.toDouble()
        if (aspectRatio > uploadConfig.maxAspectRatio) {
            return VerificationResult.AbnormalAspectRatio(dimensions.width, dimensions.height, aspectRatio)
        }

        return VerificationResult.Pass
    }

    private fun createTempFileFor(id: UUID, fileName: String?): Path {
        val extension = fileName.extensionOrEmpty().ifBlank { "upload" }
        val tempFile = tempFolder.resolve("${id}_${UUID.randomUUID()}.$extension")
        tempFile.deleteIfExists()
        tempFile.createFile()
        return tempFile
    }
}

private data class ImageDimensions(
    val width: Int,
    val height: Int,
)

private fun String?.extensionOrEmpty(): String {
    return this
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        .orEmpty()
}

private suspend fun readImageDimensions(
    bytes: ByteArray,
    format: DecodableImageFormat,
): ImageDimensions? = withContext(Dispatchers.IO) {
    val imageInput = format.inputStreamSpi?.createInputStreamInstance(bytes, false, null)
        ?: ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        ?: return@withContext null

    imageInput.use { input ->
        val reader = format.readerSpi?.createReaderInstance()
            ?: ImageIO.getImageReaders(input).asSequence().firstOrNull()?.also { input.seek(0) }
            ?: return@withContext null

        try {
            reader.input = input
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            if (width <= 0 || height <= 0) {
                return@withContext null
            }

            return@withContext ImageDimensions(width, height)
        } catch (exception: Exception) {
            logger.log(Level.FINE, "An error occurred while reading image dimensions", exception)
            return@withContext null
        } finally {
            reader.dispose()
        }
    }
}
