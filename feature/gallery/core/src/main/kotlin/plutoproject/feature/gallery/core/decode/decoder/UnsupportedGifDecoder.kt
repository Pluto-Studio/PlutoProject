package plutoproject.feature.gallery.core.decode.decoder

import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.DecodeStatus
import plutoproject.feature.gallery.core.decode.DecodedImage

object UnsupportedGifDecoder : ImageDecoder {
    override suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage> {
        return DecodeResult.failed(DecodeStatus.DECODE_FAILED)
    }
}
