package plutoproject.feature.gallery.core.decode

sealed interface DecodeResult<out T> {
    data class Success<T>(val data: T) : DecodeResult<T>
    data object InvalidImage : DecodeResult<Nothing>
    data object UnsupportedFormat: DecodeResult<Nothing>
    data object ImageTooLarge : DecodeResult<Nothing>
    data object TooManyFrames : DecodeResult<Nothing>
    data class UnknownFailure(val cause: Throwable? = null) : DecodeResult<Nothing>
}
