package plutoproject.feature.gallery.core.decode.decoder

import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.DecodedImage

fun interface ImageDecoder {
    suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage>
}
