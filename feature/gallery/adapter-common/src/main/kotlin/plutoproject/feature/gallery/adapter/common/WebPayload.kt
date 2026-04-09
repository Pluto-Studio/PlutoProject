package plutoproject.feature.gallery.adapter.common

import kotlinx.serialization.Serializable

@Serializable
data class UploadConfigResponse(
    val maxBytes: Int,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxPixels: Int,
    val minShortEdge: Int,
    val minPixels: Int,
    val maxAspectRatio: Double,
    val allowedFileExtensions: List<String>,
    val allowedMimeTypes: List<String>,
    val supportedFormatNames: List<String>,
)

@Serializable
data class UploadAcceptedResponse(
    val accepted: Boolean,
)

@Serializable
data class ErrorResponse(
    val message: String,
)
