@file:OptIn(ExperimentalPathApi::class)

package plutoproject.feature.gallery.adapter.common.upload

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.time.onTimeout
import plutoproject.feature.gallery.adapter.common.GalleryConfig
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.core.decode.ImageFormatSniffer
import plutoproject.feature.gallery.core.decode.SupportedImageFormat
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import kotlin.io.path.*
import kotlin.time.toJavaDuration
import plutoproject.feature.gallery.core.decode.ContentType as CoreContentType

private val logger by lazy { koin.get<Logger>() }

private const val TEMP_FOLDER_PREFIX = "plutoproject_gallery_"
private const val TEMP_LOCK_FILE_NAME = ".lock"
private val STALE_TEMP_FOLDER_AGE: Duration = Duration.ofMinutes(5)

class TempFolderHandle(
    val path: Path,
    private val lockChannel: FileChannel,
    private val lock: FileLock,
) {
    fun close() {
        listOf(
            runCatching { lock.release() } to "releasing temp folder lock",
            runCatching { lockChannel.close() } to "closing temp folder lock channel"
        ).forEach { (result, action) ->
            result.onFailure {
                logger.log(Level.WARNING, "An error occurred while $action", it)
            }
        }
    }

    fun cleanupAndClose() {
        listOf(
            runCatching { path.deleteRecursively() } to "deleting temp folder",
            runCatching { close() } to "closing temp folder handle"
        ).forEach { (result, action) ->
            result.onFailure {
                logger.log(Level.WARNING, "An error occurred while $action", it)
            }
        }
    }
}

internal fun initializeTempFolder(): Result<TempFolderHandle> {
    cleanupTempFolders()
    return runCatching(::createTempFolderHandle).onFailure { exception ->
        logger.log(Level.SEVERE, "An error occurred while creating temp folder", exception)
    }
}

private fun createTempFolderHandle(): TempFolderHandle {
    while (true) {
        val path = Files.createTempDirectory(TEMP_FOLDER_PREFIX)
        tryAcquireTempFolderHandle(path)?.let { return it }
        runCatching { path.deleteRecursively() }
    }
}

private fun cleanupTempFolders() {
    val tempParent = Path(System.getProperty("java.io.tmpdir"))
    runCatching {
        if (!tempParent.exists()) return@runCatching
        tempParent.listDirectoryEntries()
            .filter { it.isDirectory() && it.fileName.toString().startsWith(TEMP_FOLDER_PREFIX) }
            .filter { it.isReadable() && it.isWritable() }
            .filter { Duration.between(it.getLastModifiedTime().toInstant(), Instant.now()) > STALE_TEMP_FOLDER_AGE }
            .forEach(::cleanupTempFolder)
    }
}

private fun cleanupTempFolder(path: Path) {
    val handle = tryAcquireTempFolderHandle(path) ?: return

    try {
        try {
            path.deleteRecursively()
        } catch (_: Exception) {
            return
        }
    } finally {
        handle.close()
    }
}

private fun tryAcquireTempFolderHandle(path: Path): TempFolderHandle? {
    val lockChannel = runCatching {
        FileChannel.open(
            path.resolve(TEMP_LOCK_FILE_NAME),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        )
    }.getOrElse {
        return null
    }

    val lock = try {
        lockChannel.tryLock()
    } catch (_: OverlappingFileLockException) {
        null
    } catch (_: Exception) {
        null
    }

    if (lock == null) {
        runCatching { lockChannel.close() }
        return null
    }

    return TempFolderHandle(
        path = path,
        lockChannel = lockChannel,
        lock = lock,
    )
}

sealed interface VerificationResult {
    data object Ok : VerificationResult

    sealed interface Rejected : VerificationResult
    data class FileTooLarge(val fileSize: Long) : Rejected
    data class ImageTooLarge(val width: Int, val height: Int, val pixels: Long) : Rejected
    data class TooManyFrames(val frameCount: Int) : Rejected
    data class UnallowedExtension(val fileName: String) : Rejected
    data object UnsupportedFormat : Rejected
    data object Corrupted : Rejected
    data class Failed(val cause: Throwable?) : Rejected
}

sealed interface UploadState {
    data object Waiting : UploadState
    data object Processing : UploadState

    sealed interface Finished : UploadState
    data class Completed(val file: UploadedFile) : Finished
    data object Expired : Finished
    data object Cancelled : Finished
    data class VerificationFailed(val result: VerificationResult.Rejected) : Finished
    data class Failed(val cause: Throwable?) : Finished
}

sealed interface UploadSubmissionResult {
    data object Accepted : UploadSubmissionResult
    data object NotFound : UploadSubmissionResult
    data object Expired : UploadSubmissionResult
    data object Conflict : UploadSubmissionResult
}

sealed interface UploadSessionCreateResult {
    data class Created(val session: UploadSession) : UploadSessionCreateResult
    data class Conflict(val session: UploadSession) : UploadSessionCreateResult
}

class UploadSession(
    val id: UUID,
    val creator: UUID,
    val createdAt: Instant,
    val expireAt: Instant,
    val removeAt: Instant,
    val state: MutableStateFlow<UploadState>,
    val uploadUrl: String,
) {
    fun isWaitingUpload(): Boolean {
        return state.value is UploadState.Waiting
    }

    fun isFinished(): Boolean {
        return state.value is UploadState.Finished
    }
}

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

    suspend fun <T> usePath(block: suspend (Path) -> T): Result<T> {
        return try {
            Result.success(block(tempFile))
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "An error occurred while using uploaded file", e)
            Result.failure(e)
        } finally {
            discard()
        }
    }

    fun discard() {
        if (isDiscarded) {
            return
        }

        isDiscarded = true

        listOf(runCatching { tempFile.deleteIfExists() } to "deleting temp file").forEach { (result, action) ->
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
    private val tempFolderHandle: TempFolderHandle,
    coroutineScope: CoroutineScope,
) {
    private val tempFolder = tempFolderHandle.path

    init {
        check(tempFolder.exists()) { "Temp folder must be existed" }
        check(tempFolder.isReadable() && tempFolder.isWritable()) { "Temp folder must be readable and writable" }
    }

    private val config by koin.inject<GalleryConfig>()
    private val channel = Channel<Msg>(Channel.UNLIMITED)
    private val job = coroutineScope.launch { runLoop() }

    private suspend fun runLoop() {
        val uploadSessionsById = mutableMapOf<UUID, UploadSession>()

        while (true) {
            val nextExpire = uploadSessionsById.values
                .filter { !it.isFinished() }
                .minByOrNull { it.expireAt }
            val nextRemove = uploadSessionsById.values.minByOrNull { it.removeAt }
            val nextTimeoutActionAt = if (nextExpire != null && nextRemove != null) {
                minOf(nextExpire.expireAt, nextRemove.removeAt)
            } else {
                nextExpire?.expireAt ?: nextRemove?.removeAt
            }

            val msg: Msg? = if (nextTimeoutActionAt == null) {
                channel.receive()
            } else {
                select {
                    channel.onReceive { it }
                    val waitInterval = Duration.between(clock.instant(), nextTimeoutActionAt)
                        .coerceAtLeast(Duration.ZERO)
                    onTimeout(waitInterval) { null }
                }
            }

            if (msg != null) {
                when (msg) {
                    is Msg.CreateSession -> handleCreateSession(uploadSessionsById, msg)
                    is Msg.CreateSessionIfAbsent -> handleCreateSessionIfAbsent(uploadSessionsById, msg)
                    is Msg.GetSession -> handleGetSession(uploadSessionsById, msg)
                    is Msg.GetUnfinishedSession -> handleGetUnfinishedSession(uploadSessionsById, msg)
                    is Msg.CancelUnfinishedSession -> handleCancelUnfinishedSession(uploadSessionsById, msg)
                    is Msg.SubmitUpload -> handleSubmitUpload(uploadSessionsById, msg)
                    Msg.Stop -> {
                        handleStop(uploadSessionsById)
                        return
                    }
                }
                continue
            }

            val now = clock.instant()
            val expireDue = uploadSessionsById.filterValues { !it.isFinished() && !it.expireAt.isAfter(now) }
            val removeDue = uploadSessionsById.filterValues { !it.removeAt.isAfter(now) }

            expireDue.forEach { (_, session) ->
                session.state.value = UploadState.Expired
            }

            removeDue.forEach { (id, session) ->
                if (!session.isFinished()) {
                    session.state.value = UploadState.Cancelled
                }
                uploadSessionsById.remove(id)
            }
        }
    }

    private fun handleCreateSession(uploadSessionsById: MutableMap<UUID, UploadSession>, msg: Msg.CreateSession) {
        val session = newSession(msg.creator)
        uploadSessionsById[session.id] = session
        msg.reply.complete(session)
    }

    private fun handleCreateSessionIfAbsent(
        uploadSessionsById: MutableMap<UUID, UploadSession>,
        msg: Msg.CreateSessionIfAbsent
    ) {
        val existingSession = findUnfinishedSession(uploadSessionsById, msg.creator)
        if (existingSession != null) {
            msg.reply.complete(UploadSessionCreateResult.Conflict(existingSession))
            return
        }

        val session = newSession(msg.creator)
        uploadSessionsById[session.id] = session
        msg.reply.complete(UploadSessionCreateResult.Created(session))
    }

    private fun handleGetSession(uploadSessionsById: MutableMap<UUID, UploadSession>, msg: Msg.GetSession) {
        msg.reply.complete(uploadSessionsById[msg.sessionId])
    }

    private fun handleGetUnfinishedSession(
        uploadSessionsById: MutableMap<UUID, UploadSession>,
        msg: Msg.GetUnfinishedSession
    ) {
        msg.reply.complete(findUnfinishedSession(uploadSessionsById, msg.creator))
    }

    private fun handleCancelUnfinishedSession(
        uploadSessionsById: MutableMap<UUID, UploadSession>,
        msg: Msg.CancelUnfinishedSession
    ) {
        val session = findUnfinishedSession(uploadSessionsById, msg.creator)
        if (session != null) {
            session.state.value = UploadState.Cancelled
        }
        msg.reply.complete(session)
    }

    private suspend fun handleSubmitUpload(uploadSessionsById: MutableMap<UUID, UploadSession>, msg: Msg.SubmitUpload) {
        val session = uploadSessionsById[msg.sessionId] ?: run {
            msg.reply.complete(UploadSubmissionResult.NotFound)
            return
        }

        if (!session.state.compareAndSet(UploadState.Waiting, UploadState.Processing)) {
            val reply = when (session.state.value) {
                is UploadState.Expired -> UploadSubmissionResult.Expired
                else -> UploadSubmissionResult.Conflict
            }
            msg.reply.complete(reply)
            return
        }

        val verificationResult = withContext(Dispatchers.IO) {
            verifyFile(msg.tempFile, msg.contentType, msg.originalFileName)
        }
        val nextState = when (verificationResult) {
            VerificationResult.Ok -> UploadState.Completed(UploadedFile(msg.tempFile))
            is VerificationResult.Rejected -> UploadState.VerificationFailed(verificationResult)
        }

        session.state.value = nextState
        msg.reply.complete(UploadSubmissionResult.Accepted)
    }

    private fun handleStop(uploadSessionsById: MutableMap<UUID, UploadSession>) {
        uploadSessionsById
            .filterValues { it.state !is UploadState.Finished }
            .forEach { (_, session) ->
                session.state.value = UploadState.Cancelled
            }
        uploadSessionsById.clear()
    }

    private sealed interface Msg {
        data class CreateSession(val creator: UUID, val reply: CompletableDeferred<UploadSession>) : Msg
        data class CreateSessionIfAbsent(
            val creator: UUID,
            val reply: CompletableDeferred<UploadSessionCreateResult>
        ) : Msg
        data class GetSession(val sessionId: UUID, val reply: CompletableDeferred<UploadSession?>) : Msg
        data class GetUnfinishedSession(val creator: UUID, val reply: CompletableDeferred<UploadSession?>) : Msg
        data class CancelUnfinishedSession(val creator: UUID, val reply: CompletableDeferred<UploadSession?>) : Msg
        data class SubmitUpload(
            val sessionId: UUID,
            val tempFile: Path,
            val contentType: ContentType?,
            val originalFileName: String?,
            val reply: CompletableDeferred<UploadSubmissionResult>
        ) : Msg

        data object Stop : Msg
    }

    suspend fun createSession(creator: UUID): UploadSession {
        check(job.isActive) { "UploadService is closed" }
        val reply = CompletableDeferred<UploadSession>()
        channel.trySend(Msg.CreateSession(creator, reply))
        return reply.await()
    }

    suspend fun createSessionIfAbsent(creator: UUID): UploadSessionCreateResult {
        check(job.isActive) { "UploadService is closed" }
        val reply = CompletableDeferred<UploadSessionCreateResult>()
        channel.trySend(Msg.CreateSessionIfAbsent(creator, reply))
        return reply.await()
    }

    suspend fun getSession(sessionId: UUID): UploadSession? {
        check(job.isActive) { "UploadService is closed" }
        val reply = CompletableDeferred<UploadSession?>()
        channel.trySend(Msg.GetSession(sessionId, reply))
        return reply.await()
    }

    suspend fun getUnfinishedSession(creator: UUID): UploadSession? {
        check(job.isActive) { "UploadService is closed" }
        val reply = CompletableDeferred<UploadSession?>()
        channel.trySend(Msg.GetUnfinishedSession(creator, reply))
        return reply.await()
    }

    suspend fun cancelUnfinishedSession(creator: UUID): UploadSession? {
        check(job.isActive) { "UploadService is closed" }
        val reply = CompletableDeferred<UploadSession?>()
        channel.trySend(Msg.CancelUnfinishedSession(creator, reply))
        return reply.await()
    }

    suspend fun submitUpload(
        sessionId: UUID,
        tempFile: Path,
        contentType: ContentType?,
        originalFileName: String?
    ): UploadSubmissionResult {
        check(job.isActive) { "UploadService is closed" }
        val reply = CompletableDeferred<UploadSubmissionResult>()
        channel.trySend(Msg.SubmitUpload(sessionId, tempFile, contentType, originalFileName, reply))
        return reply.await()
    }

    suspend fun close() {
        if (!job.isActive) {
            return
        }
        channel.trySend(Msg.Stop)
        job.join()
        channel.close()
        tempFolderHandle.cleanupAndClose()
    }

    private fun newSession(creator: UUID): UploadSession {
        val id = UUID.randomUUID()
        val now = clock.instant()
        return UploadSession(
            id = id,
            creator = creator,
            createdAt = now,
            expireAt = now.plus(config.upload.requestExpireAfter.toJavaDuration()),
            removeAt = now.plus(config.upload.requestRemoveAfter.toJavaDuration()),
            state = MutableStateFlow(UploadState.Waiting),
            uploadUrl = "${config.upload.baseUrl.trimEnd('/')}/upload/$id/",
        )
    }

    private fun findUnfinishedSession(
        uploadSessionsById: Map<UUID, UploadSession>,
        creator: UUID
    ): UploadSession? {
        return uploadSessionsById.values
            .filter { it.creator == creator && !it.isFinished() }
            .maxByOrNull { it.createdAt }
    }

    fun getTempFile(sessionId: UUID): Result<Path> = runCatching {
        tempFolder.resolve("upload_$sessionId").also {
            it.deleteIfExists()
            it.createFile()
        }
    }.onFailure {
        logger.log(Level.SEVERE, "An error occurred while obtaining temp file for upload session $sessionId", it)
    }

    private fun verifyFile(
        tempFile: Path,
        contentType: ContentType?,
        originalFileName: String?
    ): VerificationResult = runCatching {
        val fileSize = tempFile.fileSize()
        if (fileSize > config.fileProcessing.maxBytes) {
            return VerificationResult.FileTooLarge(fileSize)
        }

        if (contentType != null && contentType !in SUPPORTED_MIME_TYPES) {
            return VerificationResult.UnsupportedFormat
        }

        val extension = originalFileName?.extension()
        if (extension != null && extension !in SupportedImageFormat.SUPPORTED_FILE_EXTENSIONS) {
            return VerificationResult.UnallowedExtension(originalFileName)
        }

        val format = ImageFormatSniffer.sniff(tempFile)
            ?: return VerificationResult.UnsupportedFormat


        val (width, height) = openImage(tempFile, format)
            ?.use { readImageDimension(it, format) ?: return VerificationResult.Corrupted }
            ?: return VerificationResult.UnsupportedFormat
        val pixels = width.toLong() * height.toLong()

        if (pixels > config.fileProcessing.maxPixels.toLong()) {
            return VerificationResult.ImageTooLarge(width, height, pixels)
        }

        val frameCount = openImage(tempFile, format)
            ?.use {
                if (format is SupportedImageFormat.Gif) {
                    readGifFrameCount(it) ?: return VerificationResult.Corrupted
                } else {
                    1
                }
            } ?: return VerificationResult.UnsupportedFormat
        if (frameCount > config.fileProcessing.maxFrames) {
            return VerificationResult.TooManyFrames(frameCount)
        }

        return VerificationResult.Ok
    }.getOrElse {
        logger.log(Level.SEVERE, "An error occurred while verifying uploaded file", it)
        VerificationResult.Failed(it)
    }
}

private fun String.extension(): String {
    return this
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
}

private fun openImage(tempFile: Path, format: SupportedImageFormat): ImageInputStream? {
    return format.inputStreamSpi?.createInputStreamInstance(tempFile.toFile(), false, null)
        ?: ImageIO.createImageInputStream(tempFile.toFile())
}

private fun readImageDimension(input: ImageInputStream, format: SupportedImageFormat): Pair<Int, Int>? {
    val reader = format.readerSpi?.createReaderInstance()
        ?: ImageIO.getImageReaders(input).asSequence().firstOrNull().also { input.seek(0) }
        ?: return null

    try {
        reader.input = input

        val width = reader.getWidth(0)
        val height = reader.getHeight(0)

        if (width <= 0 || height <= 0) {
            return null
        }

        return width to height
    } finally {
        reader.dispose()
    }
}

private fun readGifFrameCount(input: ImageInputStream): Int? {
    val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull().also { input.seek(0) }
        ?: return null

    try {
        reader.input = input
        return reader.getNumImages(true)
    } finally {
        reader.dispose()
    }
}
