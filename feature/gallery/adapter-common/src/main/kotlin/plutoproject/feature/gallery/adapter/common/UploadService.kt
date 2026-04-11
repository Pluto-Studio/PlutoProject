package plutoproject.feature.gallery.adapter.common

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import plutoproject.feature.gallery.core.decode.ImageFormatSniffer
import plutoproject.feature.gallery.core.decode.SupportedImageFormat
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
import plutoproject.feature.gallery.core.decode.ContentType as CoreContentType

private val config by lazy { koin.get<GalleryConfig>() }
private val logger by lazy { koin.get<Logger>() }

private const val TEMP_FOLDER_SUB = "plutoproject_gallery_"

fun initializeTempFolder(): Result<Path> {
    cleanupTempFolders()

    val sub = "$TEMP_FOLDER_SUB${UUID.randomUUID()}"
    val path = Path(config.fileProcessing.tempFolderRoot, sub)

    return runCatching {
        path.createDirectories()
        path
    }.onFailure { exception ->
        logger.log(Level.SEVERE, "An error occurred while creating temp folder", exception)
    }
}

@OptIn(ExperimentalPathApi::class)
private fun cleanupTempFolders() {
    val tempRoot = Path(config.fileProcessing.tempFolderRoot)
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
    data object Ok : VerificationResult

    sealed interface Rejected : VerificationResult
    data class FileTooLarge(val size: Int) : Rejected
    data class ImageTooLarge(val width: Int, val height: Int, val pixels: Long) : Rejected
    data class TooManyFrames(val frameCount: Int) : Rejected
    data class UnallowedExtension(val fileName: String) : Rejected
    data object UnsupportedFormat : Rejected
}

sealed interface UploadState {
    data object Waiting : UploadState
    data object Processing: UploadState

    sealed interface Finished : UploadState
    data class Completed(val file: UploadedFile) : Finished
    data object Expired : Finished
    data object Cancelled : Finished
    data class VerificationFailed(val result: VerificationResult.Rejected) : UploadState
    data class Failed(val cause: Throwable?) : UploadState
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

private fun CoreContentType.toKtor(): ContentType {
    return ContentType(contentType, contentSubType)
}

private val SUPPORTED_MIME_TYPES = SupportedImageFormat.SUPPORTED_MIME_TYPES.map { it.toKtor() }

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

    private val config by koin.inject<GalleryConfig>()
    private val allowedExtensions = config.fileProcessing.allowedFileExtensions.map { it.lowercase() }.toSet()

    fun createSession(creator: UUID): UploadSession {
        check(!isClosed) { "UploadService is closed" }
        val id = UUID.randomUUID()
        return UploadSession(
            id = id,
            creator = creator,
            createdAt = clock.instant(),
            state = MutableStateFlow(UploadState.Waiting),
            uploadUrl = "${config.upload.baseUrl.trimEnd('/')}/upload/$id",
            expiryJob = createExpiryJob(id)
        ).also {
            uploadSessionsById[id] = it
        }
    }

    private fun createExpiryJob(sessionId: UUID): Job {
        return coroutineScope.launch {
            delay(config.upload.requestExpireAfter)
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
        if (verificationResult is VerificationResult.Rejected) {
            updateUploadSession(id, UploadState.VerificationFailed(verificationResult))
            return UploadSubmissionResult.Accepted
        }

        return runCatching {
            val tempFile = createTempFileFor(id, fileName)
            tempFile.writeBytes(bytes)
            updateUploadSession(id, UploadState.Completed(UploadedFile(tempFile)))
            UploadSubmissionResult.Accepted
        }.getOrElse { exception ->
            logger.log(Level.SEVERE, "An error occurred while storing uploaded file", exception)
            updateUploadSession(id, UploadState.Failed(exception))
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
            if (session.state.value !is UploadState.Completed) {
                session.state.value = UploadState.Cancelled
                return@forEach
            }
            val uploadedFile = (session.state.value as UploadState.Completed).file
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
        if (bytes.size > config.fileProcessing.maxBytes) {
            return VerificationResult.FileTooLarge(bytes.size)
        }

        val extension = fileName.extensionOrEmpty()
        if (extension !in allowedExtensions) {
            return VerificationResult.UnallowedExtension(fileName.orEmpty())
        }

        if (contentType != null && contentType !in SUPPORTED_MIME_TYPES) {
            return VerificationResult.UnsupportedFormat
        }

        val format = ImageFormatSniffer.sniff(bytes, fileName) ?: return VerificationResult.UnsupportedFormat
        val dimensions = readImageDimensions(bytes, format) ?: return VerificationResult.UnsupportedFormat
        val pixels = dimensions.width.toLong() * dimensions.height.toLong()

        if (pixels > config.fileProcessing.maxPixels.toLong() || pixels > Int.MAX_VALUE.toLong()) {
            return VerificationResult.ImageTooLarge(dimensions.width, dimensions.height, pixels)
        }

        return VerificationResult.Ok
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
    format: SupportedImageFormat,
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
