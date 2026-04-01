package plutoproject.feature.gallery.core.decode.animated.gif

import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import plutoproject.feature.gallery.core.decode.animated.SourceFrameMetadata
import plutoproject.feature.gallery.core.util.checkpoint
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal data class GifFrameDescriptor(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal enum class GifDisposalMethod {
    NONE,
    RESTORE_TO_BACKGROUND,
    RESTORE_TO_PREVIOUS,
}

internal fun readFrameDescriptor(metadata: IIOMetadata): GifFrameDescriptor {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull()
        ?: return GifFrameDescriptor(left = 0, top = 0, width = -1, height = -1)
    val imageDescriptor = root.findFirstNode("ImageDescriptor")
        ?: return GifFrameDescriptor(left = 0, top = 0, width = -1, height = -1)
    val attributes = imageDescriptor.attributes
    return GifFrameDescriptor(
        left = attributes.readInt("imageLeftPosition") ?: 0,
        top = attributes.readInt("imageTopPosition") ?: 0,
        width = attributes.readInt("imageWidth") ?: -1,
        height = attributes.readInt("imageHeight") ?: -1,
    )
}

internal fun readDisposalMethod(metadata: IIOMetadata): GifDisposalMethod {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull()
        ?: return GifDisposalMethod.NONE
    val graphicControl = root.findFirstNode("GraphicControlExtension") ?: return GifDisposalMethod.NONE
    return when (graphicControl.attributes.readString("disposalMethod")) {
        "restoreToBackgroundColor" -> GifDisposalMethod.RESTORE_TO_BACKGROUND
        "restoreToPrevious" -> GifDisposalMethod.RESTORE_TO_PREVIOUS
        "none", "doNotDispose" -> GifDisposalMethod.NONE
        else -> GifDisposalMethod.NONE
    }
}

internal data class IntRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal fun resolvePatchRect(
    descriptor: GifFrameDescriptor,
    patchWidth: Int,
    patchHeight: Int,
): IntRect {
    val width = if (descriptor.width > 0) descriptor.width else patchWidth
    val height = if (descriptor.height > 0) descriptor.height else patchHeight
    return IntRect(
        left = descriptor.left,
        top = descriptor.top,
        width = width,
        height = height,
    )
}

internal fun drawPatch(
    canvas: IntArray,
    canvasWidth: Int,
    canvasHeight: Int,
    patchPixels: IntArray,
    patchWidth: Int,
    patchHeight: Int,
    patchLeft: Int,
    patchTop: Int,
) {
    val clippedLeft = patchLeft.coerceIn(0, canvasWidth)
    val clippedTop = patchTop.coerceIn(0, canvasHeight)
    val clippedRight = (patchLeft + patchWidth).coerceIn(0, canvasWidth)
    val clippedBottom = (patchTop + patchHeight).coerceIn(0, canvasHeight)

    if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
        return
    }

    for (y in clippedTop until clippedBottom) {
        val sourceY = y - patchTop
        val sourceBase = sourceY * patchWidth
        val destinationBase = y * canvasWidth
        for (x in clippedLeft until clippedRight) {
            val sourceX = x - patchLeft
            val sourcePixel = patchPixels[sourceBase + sourceX]
            if ((sourcePixel ushr 24) != 0) {
                canvas[destinationBase + x] = sourcePixel
            }
        }
    }
}

internal fun applyDisposal(
    method: GifDisposalMethod,
    canvas: IntArray,
    canvasWidth: Int,
    canvasHeight: Int,
    patchRect: IntRect,
    snapshotBeforeDraw: IntArray?,
) {
    when (method) {
        GifDisposalMethod.NONE -> Unit
        GifDisposalMethod.RESTORE_TO_BACKGROUND -> clearRect(canvas, canvasWidth, canvasHeight, patchRect)
        GifDisposalMethod.RESTORE_TO_PREVIOUS -> {
            if (snapshotBeforeDraw != null && snapshotBeforeDraw.size == canvas.size) {
                snapshotBeforeDraw.copyInto(canvas)
            }
        }
    }
}

private fun clearRect(
    canvas: IntArray,
    canvasWidth: Int,
    canvasHeight: Int,
    rect: IntRect,
) {
    val clippedLeft = rect.left.coerceIn(0, canvasWidth)
    val clippedTop = rect.top.coerceIn(0, canvasHeight)
    val clippedRight = (rect.left + rect.width).coerceIn(0, canvasWidth)
    val clippedBottom = (rect.top + rect.height).coerceIn(0, canvasHeight)

    if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
        return
    }

    for (y in clippedTop until clippedBottom) {
        val base = y * canvasWidth
        for (x in clippedLeft until clippedRight) {
            canvas[base + x] = 0x00000000
        }
    }
}

internal suspend fun scanSourceFrameMetadata(
    reader: ImageReader,
    frameCount: Int,
): List<SourceFrameMetadata> {
    val frameTimeline = ArrayList<SourceFrameMetadata>(frameCount)

    for (frameIndex in 0 until frameCount) {
        checkpoint()
        val metadata = reader.getImageMetadata(frameIndex)
        frameTimeline += SourceFrameMetadata(
            sourceFrameIndex = frameIndex,
            duration = readFrameDuration(metadata),
        )
    }

    return frameTimeline
}

internal fun readLogicalScreenSize(reader: ImageReader): IntSize {
    val streamMetadata = reader.streamMetadata ?: return IntSize(width = -1, height = -1)
    val root = runCatching { streamMetadata.getAsTree("javax_imageio_gif_stream_1.0") }.getOrNull()
        ?: return IntSize(width = -1, height = -1)
    val logicalScreen = root.findFirstNode("LogicalScreenDescriptor") ?: return IntSize(width = -1, height = -1)
    val attributes = logicalScreen.attributes
    return IntSize(
        width = attributes.readInt("logicalScreenWidth") ?: -1,
        height = attributes.readInt("logicalScreenHeight") ?: -1,
    )
}

internal fun readFrameDuration(metadata: IIOMetadata): Duration {
    return (readDelayCentiseconds(metadata).toLong() * 10L).milliseconds
}

internal data class IntSize(
    val width: Int,
    val height: Int,
)

private fun readDelayCentiseconds(metadata: IIOMetadata): Int {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull() ?: return 0
    val graphicControl = root.findFirstNode("GraphicControlExtension") ?: return 0
    return graphicControl.attributes.readInt("delayTime")?.coerceAtLeast(0) ?: 0
}

internal fun Node.findFirstNode(nodeName: String): Node? {
    if (this.nodeName == nodeName) {
        return this
    }

    val children = childNodes
    for (index in 0 until children.length) {
        val found = children.item(index).findFirstNode(nodeName)
        if (found != null) {
            return found
        }
    }

    return null
}

internal fun NamedNodeMap.readInt(attributeName: String): Int? = readString(attributeName)?.toIntOrNull()

internal fun NamedNodeMap.readString(attributeName: String): String? = getNamedItem(attributeName)?.nodeValue
