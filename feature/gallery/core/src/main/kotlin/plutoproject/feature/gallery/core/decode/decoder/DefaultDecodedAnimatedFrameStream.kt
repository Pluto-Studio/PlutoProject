package plutoproject.feature.gallery.core.decode.decoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.decode.AnimatedFrameTiming
import plutoproject.feature.gallery.core.decode.DecodedAnimatedFrame
import plutoproject.feature.gallery.core.decode.DecodedAnimatedFrameStream
import plutoproject.feature.gallery.core.render.RgbaImage8888
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream

internal class DefaultDecodedAnimatedFrameStream(
    internal val reader: ImageReader,
    internal val imageInput: ImageInputStream,
    internal val width: Int,
    internal val height: Int,
    internal val frameTimeline: List<AnimatedFrameTiming>,
) : DecodedAnimatedFrameStream {
    internal val canvas = IntArray(width * height)
    internal var nextFrameIndex = 0
    internal var closed = false

    override suspend fun nextFrame(): DecodedAnimatedFrame? {
        check(!closed) { "frame stream is already closed" }
        if (nextFrameIndex >= frameTimeline.size) {
            return null
        }

        checkpoint()

        val metadata = withContext(Dispatchers.IO) {
            reader.getImageMetadata(nextFrameIndex)
        }
        val descriptor = readFrameDescriptor(metadata)
        val disposalMethod = readDisposalMethod(metadata)

        val patchImage = withContext(Dispatchers.IO) {
            reader.read(nextFrameIndex)
        } ?: throw IllegalArgumentException("failed to read gif frame")
        val patchWidth = patchImage.width
        val patchHeight = patchImage.height
        if (patchWidth <= 0 || patchHeight <= 0) {
            throw IllegalArgumentException("invalid gif frame dimensions")
        }

        val patchPixels = IntArray(patchWidth * patchHeight)
        patchImage.getRGB(0, 0, patchWidth, patchHeight, patchPixels, 0, patchWidth)

        val patchRect = resolvePatchRect(descriptor, patchWidth, patchHeight)
        val snapshotBeforeDraw = if (disposalMethod == GifDisposalMethod.RESTORE_TO_PREVIOUS) {
            canvas.copyOf()
        } else {
            null
        }

        drawPatch(
            canvas = canvas,
            canvasWidth = width,
            canvasHeight = height,
            patchPixels = patchPixels,
            patchWidth = patchWidth,
            patchHeight = patchHeight,
            patchLeft = patchRect.left,
            patchTop = patchRect.top,
        )

        val timing = frameTimeline[nextFrameIndex]
        val frame = DecodedAnimatedFrame(
            sourceFrameIndex = timing.sourceFrameIndex,
            delayCentiseconds = timing.delayCentiseconds,
            image = RgbaImage8888(width = width, height = height, pixels = canvas.copyOf()),
        )

        applyDisposal(
            method = disposalMethod,
            canvas = canvas,
            canvasWidth = width,
            canvasHeight = height,
            patchRect = patchRect,
            snapshotBeforeDraw = snapshotBeforeDraw,
        )

        nextFrameIndex++
        return frame
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        reader.dispose()
        imageInput.close()
    }
}
