package plutoproject.feature.gallery.core.render

/**
 * 静态图渲染输入。
 */
data class RenderStaticImageRequest(
    val sourceImage: RgbaImage8888,
    val mapXBlocks: Int,
    val mapYBlocks: Int,
    val profile: RenderProfile,
)

/**
 * 动图源帧。
 *
 * [delayCentiseconds] 的单位为 1/100 秒（GIF 原始单位）。
 */
data class AnimatedSourceFrame(
    val image: RgbaImage8888,
    /**
     * GIF 原始帧延迟。
     *
     * - 单位：`1/100s`（centisecond）
     * - 允许为 0（部分 GIF 会这样写），渲染端会使用 [RenderProfile.minFrameDelayMillis] 做 clamp
     */
    val delayCentiseconds: Int,
) {
    init {
        require(delayCentiseconds >= 0) { "delayCentiseconds must be >= 0" }
    }
}

/**
 * 动图渲染输入。
 *
 * [sourceFrames] 需按原始时间顺序排列（第 0 帧到最后一帧）。
 */
data class RenderAnimatedImageRequest(
    val sourceFrames: List<AnimatedSourceFrame>,
    val mapXBlocks: Int,
    val mapYBlocks: Int,
    val profile: RenderProfile,
)
