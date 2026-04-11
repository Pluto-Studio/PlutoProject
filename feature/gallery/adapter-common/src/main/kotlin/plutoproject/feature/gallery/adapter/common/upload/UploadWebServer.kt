package plutoproject.feature.gallery.adapter.common.upload

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import plutoproject.feature.gallery.adapter.common.GalleryConfig
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.core.decode.SupportedImageFormat
import plutoproject.framework.common.util.data.uuidOrNull
import java.io.OutputStream
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

private val config by lazy { koin.get<GalleryConfig>() }
private val uploadService by lazy { koin.get<UploadService>() }
private val logger by lazy { koin.get<Logger>() }
private val resourceClassLoader = object {}.javaClass.classLoader
private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

private const val GALLERY_RESOURCE_PREFIX = "/gallery_frontend"

fun startWebServer() {
    if (engine != null) {
        return
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEnvironment {
            classLoader = resourceClassLoader
        },
        configure = {
            connector {
                port = config.upload.port
            }
        },
        module = webModule()
    )
    server.start(wait = false)
    engine = server

    logger.info("Gallery web server started on port ${config.upload.port}")
}

fun stopWebServer() {
    engine?.stop(1_000, 3_000)
    engine = null
}

private fun webModule(): Application.() -> Unit = {
    routing {
        apiRoutes()
        uploadPageRoute()
        staticResources("/assets", "$GALLERY_RESOURCE_PREFIX/assets/")
    }
}

private fun Route.apiRoutes() {
    route("/api") {
        get("/config") {
            call.respondJson(buildConfigResponse())
        }
        route("/sessions/{id}") {
            post("/upload", RoutingContext::handleUpload)
        }
    }
}

private suspend fun RoutingContext.handleUpload() {
    val id = call.parameters["id"]?.uuidOrNull()
        ?: return call.respond(HttpStatusCode.BadRequest)
    val session = uploadService.getSession(id)
        ?: return call.respond(HttpStatusCode.NotFound)

    if (!session.isWaitingUpload()) {
        return call.respond(HttpStatusCode.Gone)
    }

    val multipart = call.receiveMultipart()
    var storedPath: Path? = null
    var contentType: ContentType? = null
    var originalFileName: String? = null

    while (true) {
        val part = multipart.readPart() ?: break
        try {
            if (part !is PartData.FileItem) {
                continue
            }

            val tempFile = uploadService.getTempFile(id).getOrNull()
                ?: return call.respond(HttpStatusCode.InternalServerError)

            val isSizeAcceptable = tempFile.outputStream().use { output ->
                readAndWriteFile(part.provider(), output)
            }

            if (!isSizeAcceptable) {
                tempFile.deleteIfExists()
                return call.respond(HttpStatusCode.PayloadTooLarge)
            }

            storedPath = tempFile
            originalFileName = part.originalFileName
            contentType = part.contentType
            break
        } finally {
            part.dispose()
        }
    }

    if (storedPath == null) {
        return call.respond(HttpStatusCode.BadRequest)
    }

    val result = uploadService.submitUpload(id, storedPath, contentType, originalFileName)

    when (result) {
        UploadSubmissionResult.Accepted -> call.respond(HttpStatusCode.OK)
        UploadSubmissionResult.Conflict -> call.respond(HttpStatusCode.Conflict)
        UploadSubmissionResult.Expired -> call.respond(HttpStatusCode.Gone)
        UploadSubmissionResult.NotFound -> call.respond(HttpStatusCode.NotFound)
    }
}

private suspend fun readAndWriteFile(channel: ByteReadChannel, output: OutputStream): Boolean {
    var total = 0L
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    while (true) {
        val read = channel.readAvailable(buffer)
        if (read == -1) break

        total += read
        if (total > config.fileProcessing.maxBytes) {
            channel.cancel()
            return false
        }

        withContext(Dispatchers.IO) {
            output.write(buffer, 0, read)
        }
    }

    return true
}

private fun Route.uploadPageRoute() {
    get("/upload/{id}") {
        val id = call.parameters["id"]?.uuidOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val session = uploadService.getSession(id)
            ?: return@get call.respond(HttpStatusCode.NotFound)

        if (!session.isWaitingUpload()) {
            return@get call.respond(HttpStatusCode.Gone)
        }

        call.respondResource("$GALLERY_RESOURCE_PREFIX/index.html")
    }
}

private suspend inline fun <reified T> ApplicationCall.respondJson(response: T) {
    respondText(json.encodeToString(response), ContentType.Application.Json, HttpStatusCode.OK)
}

@Serializable
data class UploadConfigResponse(
    val maxBytes: Long,
    val maxPixels: Int,
    val supportedFileExtensions: List<String>,
    val supportedMimeTypes: List<String>,
    val supportedFormatNames: List<String>,
)

private fun buildConfigResponse() = UploadConfigResponse(
    maxBytes = config.fileProcessing.maxBytes,
    maxPixels = config.fileProcessing.maxPixels,
    supportedFileExtensions = SupportedImageFormat.SUPPORTED_FILE_EXTENSIONS,
    supportedMimeTypes = SupportedImageFormat.SUPPORTED_MIME_TYPES.map { "${it.contentType}/${it.contentSubType}" },
    supportedFormatNames = SupportedImageFormat.SUPPORTED_FORMAT_NAMES,
)
