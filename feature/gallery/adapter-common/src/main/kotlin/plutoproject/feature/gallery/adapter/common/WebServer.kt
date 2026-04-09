package plutoproject.feature.gallery.adapter.common

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.json.Json
import plutoproject.framework.common.util.featureDataFolder
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.createDirectories

private val galleryConfig by lazy { koin.get<GalleryConfig>() }
private val uploadService by lazy { koin.get<UploadService>() }
private val logger by lazy { koin.get<Logger>() }
private val webRoot = featureDataFolder.resolve("gallery/web").toPath().also { it.createDirectories() }
private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

fun startWebServer() {
    if (engine != null) {
        return
    }

    webRoot.createDirectories()

    val server = embeddedServer(
        factory = Netty,
        port = galleryConfig.upload.port,
        module = createWebModule()
    )
    server.start(wait = false)
    engine = server

    logger.info("Gallery web server started on port ${galleryConfig.upload.port}")
}

fun stopWebServer() {
    engine?.stop(1_000, 3_000)
    engine = null
}

private fun createWebModule(): Application.() -> Unit = {
    routing {
        uploadSessionRoutes()
        uploadPageRoute(webRoot)
        staticFiles("/", webRoot.toFile()) {
            default("index.html")
        }
    }
}

private fun Route.uploadSessionRoutes() {
    route("/api/upload-sessions/{id}") {
        get("/config") {
            handleGetUploadConfig(call)
        }
        post("/file") {
            handleUploadFile(call)
        }
    }
}

private fun Route.uploadPageRoute(webRoot: Path) {
    get("/upload/{id}") {
        val indexFile = webRoot.resolve("index.html").toFile()
        if (!indexFile.exists()) {
            call.respondText(
                "未找到网页文件，请将静态前端产物放入 plugins/PlutoProject/feature/gallery/web",
                status = HttpStatusCode.NotFound,
            )
            return@get
        }

        call.respondFile(indexFile)
    }
}

private suspend fun handleGetUploadConfig(call: ApplicationCall) {
    val session = call.sessionOrRespond() ?: return
    if (session.state.value == UploadState.Expired) {
        call.respondJson(HttpStatusCode.Gone, ErrorResponse("上传会话已过期"))
        return
    }

    call.respondJson(buildConfigPayload())
}

private suspend fun handleUploadFile(call: ApplicationCall) {
    val session = call.sessionOrRespond() ?: return
    if (session.state.value == UploadState.Expired) {
        call.respondJson(HttpStatusCode.Gone, ErrorResponse("上传会话已过期"))
        return
    }

    val uploadedFile = call.readUploadedFile()
    if (uploadedFile == null) {
        call.respondJson(HttpStatusCode.BadRequest, ErrorResponse("缺少上传文件"))
        return
    }

    when (val result =
        uploadService.submitUpload(session.id, uploadedFile.fileName, uploadedFile.contentType, uploadedFile.bytes)) {
        UploadSubmissionResult.Accepted -> {
            call.respondJson(HttpStatusCode.Accepted, UploadAcceptedResponse(accepted = true))
        }

        UploadSubmissionResult.Conflict -> {
            call.respondJson(HttpStatusCode.Conflict, ErrorResponse("当前上传会话不可重复提交"))
        }

        UploadSubmissionResult.Expired -> {
            call.respondJson(HttpStatusCode.Gone, ErrorResponse("上传会话已过期"))
        }

        is UploadSubmissionResult.Failed -> {
            logger.log(Level.SEVERE, "An error occurred while handling upload request", result.cause)
            call.respondJson(HttpStatusCode.InternalServerError, ErrorResponse("服务器暂时无法处理该请求"))
        }

        UploadSubmissionResult.NotFound -> {
            call.respondJson(HttpStatusCode.NotFound, ErrorResponse("上传会话不存在"))
        }
    }
}

private suspend fun ApplicationCall.readUploadedFile(): UploadedMultipartFile? {
    var fileName: String? = null
    var contentType: io.ktor.http.ContentType? = null
    var bytes: ByteArray? = null

    val multipart = receiveMultipart(formFieldLimit = galleryConfig.upload.maxBytes.toLong() + 1024)
    while (true) {
        val part = multipart.readPart() ?: break
        when (part) {
            is PartData.FileItem -> {
                if (part.name == "file" && bytes == null) {
                    fileName = part.originalFileName
                    contentType = part.contentType
                    bytes = part.provider().toInputStream().readBytes()
                }
            }

            else -> Unit
        }
        part.dispose()
    }

    val uploadedBytes = bytes ?: return null
    return UploadedMultipartFile(fileName, contentType, uploadedBytes)
}

private suspend fun ApplicationCall.sessionOrRespond(): UploadSession? {
    val id = parameters["id"]?.let {
        runCatching { UUID.fromString(it) }.getOrNull()
    }
    if (id == null) {
        respondJson(HttpStatusCode.BadRequest, ErrorResponse("上传会话 ID 不合法"))
        return null
    }

    val session = uploadService.findUploadSession(id)
    if (session == null) {
        respondJson(HttpStatusCode.NotFound, ErrorResponse("上传会话不存在"))
        return null
    }

    return session
}

private suspend inline fun <reified T> ApplicationCall.respondJson(payload: T) {
    respondJson(HttpStatusCode.OK, payload)
}

private suspend inline fun <reified T> ApplicationCall.respondJson(status: HttpStatusCode, payload: T) {
    respondText(
        text = json.encodeToString(payload),
        contentType = ContentType.Application.Json,
        status = status,
    )
}

private fun buildConfigPayload() = UploadConfigResponse(
    maxBytes = galleryConfig.upload.maxBytes,
    maxWidth = galleryConfig.upload.maxWidth,
    maxHeight = galleryConfig.upload.maxHeight,
    maxPixels = galleryConfig.upload.maxPixels,
    minShortEdge = galleryConfig.upload.minShortEdge,
    minPixels = galleryConfig.upload.minPixels,
    maxAspectRatio = galleryConfig.upload.maxAspectRatio,
    allowedFileExtensions = galleryConfig.upload.allowedFileExtensions,
    allowedMimeTypes = galleryConfig.upload.allowedFileExtensions.mapNotNull(::mimeTypeFor).distinct(),
    supportedFormatNames = galleryConfig.upload.supportedFormatNames,
)

private data class UploadedMultipartFile(
    val fileName: String?,
    val contentType: io.ktor.http.ContentType?,
    val bytes: ByteArray,
)

private fun mimeTypeFor(extension: String): String? {
    return when (extension.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> null
    }
}
