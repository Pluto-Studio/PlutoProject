package plutoproject.feature.gallery.adapter.common

import kotlinx.coroutines.flow.StateFlow
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.render.RenderResult
import java.util.*
import kotlin.time.Instant

sealed interface UploadRequestState {
    data object WaitingUpload : UploadRequestState
    data object DecodingImage : UploadRequestState
    data class DecodeFailed(val result: DecodeResult<*>) : UploadRequestState
    data object RenderingImage : UploadRequestState
    data class RenderFailed(val result: RenderResult<*>) : UploadRequestState
    data class Finished(val decodedData: ImageData) : UploadRequestState
    data object Expired : UploadRequestState
    data object Cancelled : UploadRequestState
    data class UnknownError(val cause: Throwable?) : UploadRequestState
}

data class UploadRequest(
    val id: UUID,
    val creator: UUID,
    val createdAt: Instant,
    val state: StateFlow<UploadRequestState>,
    val uploadUrl: String,
)
