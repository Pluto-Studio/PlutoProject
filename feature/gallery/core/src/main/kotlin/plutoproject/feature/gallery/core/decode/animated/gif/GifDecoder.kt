package plutoproject.feature.gallery.core.decode.animated.gif

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageMetadata
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import java.io.ByteArrayInputStream
import java.io.EOFException
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

object GifDecoder {
    suspend fun decode(
        bytes: ByteArray,
        constraints: DecodeConstraints,
    ): DecodeResult<AnimatedImageSource> = try {
        if (bytes.size > constraints.maxBytes) {
            return DecodeResult.ImageTooLarge
        }

        val imageInput = withContext(Dispatchers.IO) {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        } ?: return DecodeResult.UnsupportedFormat

        imageInput.use { input ->
            decodeImageInput(bytes, input, constraints)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: EOFException) {
        DecodeResult.InvalidImage
    } catch (_: IIOException) {
        DecodeResult.InvalidImage
    } catch (e: Throwable) {
        DecodeResult.UnknownFailure(e)
    }
}

private suspend fun decodeImageInput(
    bytes: ByteArray,
    input: ImageInputStream,
    constraints: DecodeConstraints,
): DecodeResult<AnimatedImageSource> {
    val reader = withContext(Dispatchers.IO) {
        ImageIO.getImageReadersByFormatName("gif").asSequence().firstOrNull()
    } ?: return DecodeResult.UnsupportedFormat

    try {
        reader.input = input

        val screenSize = readLogicalScreenSize(reader)
        val framePixelCount = screenSize.width.toLong() * screenSize.height.toLong()

        if (screenSize.width <= 0 || screenSize.height <= 0 || framePixelCount > Int.MAX_VALUE.toLong()) {
            return DecodeResult.InvalidImage
        }

        if (framePixelCount > constraints.maxPixels.toLong()) {
            return DecodeResult.ImageTooLarge
        }

        val frameCount = withContext(Dispatchers.IO) {
            reader.getNumImages(true)
        }

        if (frameCount <= 0) {
            return DecodeResult.InvalidImage
        }
        if (frameCount > constraints.maxFrames) {
            return DecodeResult.TooManyFrames
        }

        val metadata = AnimatedImageMetadata(
            width = screenSize.width,
            height = screenSize.height,
            frameCount = frameCount,
            sourceFrameTimeline = scanSourceFrameMetadata(reader, frameCount),
        )

        return DecodeResult.Success(
            AnimatedImageSource(
                metadata = metadata,
                frameStreamOpener = { openFrameStream(bytes, metadata) },
            )
        )
    } finally {
        reader.dispose()
    }
}

private suspend fun openFrameStream(
    bytes: ByteArray,
    metadata: AnimatedImageMetadata,
): GifFrameStream {
    val imageInput = withContext(Dispatchers.IO) {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
    } ?: error("Failed to create GIF image input stream")

    val reader = runCatching {
        withContext(Dispatchers.IO) {
            ImageIO.getImageReadersByFormatName("gif").asSequence().firstOrNull()
        } ?: error("Failed to create GIF image reader")
    }.onFailure {
        imageInput.close()
    }.getOrThrow()

    reader.input = imageInput
    return GifFrameStream(reader, imageInput, metadata)
}
