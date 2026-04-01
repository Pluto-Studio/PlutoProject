package plutoproject.feature.gallery.core.decode.animated.gif

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageFrame
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageFrameStream
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageMetadata
import plutoproject.feature.gallery.core.render.PixelBuffer
import plutoproject.feature.gallery.core.util.checkpoint
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream

class GifFrameStream(
    private val reader: ImageReader,
    private val imageInput: ImageInputStream,
    private val metadata: AnimatedImageMetadata,
) : AnimatedImageFrameStream {
    private val canvas = IntArray(metadata.width * metadata.height)
    private var nextFrameIndex = 0
    private var closed = false

    override suspend fun nextFrame(): AnimatedImageFrame? {
        check(!closed) { "frame stream is already closed" }
        if (nextFrameIndex >= metadata.frameCount) {
            return null
        }

        checkpoint()

        val imageMetadata = withContext(Dispatchers.IO) {
            reader.getImageMetadata(nextFrameIndex)
        }
        val descriptor = readFrameDescriptor(imageMetadata)
        val disposalMethod = readDisposalMethod(imageMetadata)

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
            canvasWidth = metadata.width,
            canvasHeight = metadata.height,
            patchPixels = patchPixels,
            patchWidth = patchWidth,
            patchHeight = patchHeight,
            patchLeft = patchRect.left,
            patchTop = patchRect.top,
        )

        val frameMetadata = metadata.sourceFrameTimeline[nextFrameIndex]
        val frame = AnimatedImageFrame(
            sourceFrameIndex = frameMetadata.sourceFrameIndex,
            pixelBuffer = PixelBuffer(
                width = metadata.width,
                height = metadata.height,
                pixels = canvas.copyOf(),
            ),
        )

        applyDisposal(
            method = disposalMethod,
            canvas = canvas,
            canvasWidth = metadata.width,
            canvasHeight = metadata.height,
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
