package plutoproject.feature.gallery.adapter.common

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.*

private val config by lazy { koin.get<GalleryConfig>() }
private val logger by lazy { koin.get<Logger>() }

fun createTempFolder(): Result<Path> {
    val pathString = config.upload.tempFolder.replace($$"$id", UUID.randomUUID().toString())
    val path = Path.of(pathString)

    return runCatching {
        path.deleteIfExists()
        path.createDirectories()
        path
    }.onFailure { exception ->
        logger.log(Level.SEVERE, "An error occurred while creating temp folder", exception)
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
    data class VerificationFailure(val result: VerificationResult) : UploadState
}

class UploadSession(
    val id: UUID,
    val creator: UUID,
    val createdAt: Instant,
    val state: MutableStateFlow<UploadState>,
    val uploadUrl: String,
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
) {
    init {
        check(tempFolder.exists()) { "Temp folder must be existed" }
        check(tempFolder.isReadable() && tempFolder.isWritable()) { "Temp folder must be readable and writable" }
    }

    private val uploadSessionsById = ConcurrentHashMap<UUID, UploadSession>()

    fun createUploadSession(creator: UUID): UploadSession {
        val session = UploadSession(
            id = UUID.randomUUID(),
            creator = creator,
            createdAt = clock.instant(),
            state = MutableStateFlow(UploadState.Waiting),
            uploadUrl = "" // TODO: 获取实际的 URL
        )
        TODO()
    }

    private fun updateUploadSession(id: UUID, newState: UploadState) {
        val session = uploadSessionsById[id] ?: return
        session.state.value = newState
    }
}
