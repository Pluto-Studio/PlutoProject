package plutoproject.feature.gallery.core.decode

import plutoproject.feature.gallery.core.render.RgbaImage8888

data class AnimatedFrameTiming(
    val sourceFrameIndex: Int,
    val delayCentiseconds: Int,
) {
    init {
        require(sourceFrameIndex >= 0) { "sourceFrameIndex must be >= 0" }
        require(delayCentiseconds >= 0) { "delayCentiseconds must be >= 0" }
    }
}

data class DecodedAnimatedFrame(
    val sourceFrameIndex: Int,
    val delayCentiseconds: Int,
    val image: RgbaImage8888,
) {
    init {
        require(sourceFrameIndex >= 0) { "sourceFrameIndex must be >= 0" }
        require(delayCentiseconds >= 0) { "delayCentiseconds must be >= 0" }
    }
}

interface DecodedAnimatedFrameStream : AutoCloseable {
    suspend fun nextFrame(): DecodedAnimatedFrame?
}

interface DecodedAnimatedImageSource {
    val width: Int
    val height: Int
    val frameCount: Int
    val frameTimeline: List<AnimatedFrameTiming>

    suspend fun openFrameStream(): DecodedAnimatedFrameStream
}

sealed interface DecodedImage {
    data class Static(
        val image: RgbaImage8888,
    ) : DecodedImage

    data class Animated(
        val source: DecodedAnimatedImageSource,
    ) : DecodedImage
}

data class DecodeImageRequest(
    val bytes: ByteArray,
    val fileNameHint: String? = null,
    val constraints: DecodeConstraints,
)

data class DecodeConstraints(
    val maxBytes: Int,
    val maxPixels: Int,
    val maxFrames: Int,
) {
    init {
        require(maxBytes > 0) { "maxBytes must be > 0" }
        require(maxPixels > 0) { "maxPixels must be > 0" }
        require(maxFrames > 0) { "maxFrames must be > 0" }
    }
}

enum class DecodeStatus {
    /** 解码成功。 */
    SUCCEED,

    /**
     * 输入不是当前支持的图片格式。
     *
     * 典型原因：magic bytes 与扩展名提示都无法识别为 PNG/JPEG/WEBP/GIF。
     */
    UNSUPPORTED_FORMAT,

    /**
     * 图片格式看似可识别，但内容无效或已损坏。
     *
     * 典型原因：文件截断、结构损坏、解码器无法读取必要元数据。
     */
    INVALID_IMAGE,

    /**
     * 解码出的图像尺寸超出约束。
     *
     * 典型原因：输入字节数超出 `maxBytes`，或 `width * height` 超出 `maxPixels`。
     */
    IMAGE_TOO_LARGE,

    /**
     * 动图帧数超出约束。
     *
     * 典型原因：GIF 帧数大于 `maxFrames`。
     */
    TOO_MANY_FRAMES,

    /**
     * 解码流程执行失败，但失败原因未细分为其他状态码。
     *
     * 典型原因：内部异常、实现缺陷、依赖行为异常。
     */
    DECODE_FAILED,
}

sealed class DecodeResult<T> {
    abstract val status: DecodeStatus
    abstract val data: T?

    data class Failure<T>(
        override val status: DecodeStatus,
    ) : DecodeResult<T>() {
        init {
            require(status != DecodeStatus.SUCCEED) { "failed status cannot be SUCCEED" }
        }

        override val data: T? = null
    }

    data class Success<T>(override val data: T?) : DecodeResult<T>() {
        override val status: DecodeStatus = DecodeStatus.SUCCEED

        init {
            require(status == DecodeStatus.SUCCEED) { "success status must be SUCCEED" }
        }
    }
}
