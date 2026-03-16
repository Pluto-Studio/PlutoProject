package plutoproject.feature.gallery.core.render

/**
 * 固定渲染骨架下的算法配置集合。
 *
 * 该配置只决定每个步骤采用的实现与参数，不允许重排步骤顺序。
 */
data class RenderProfile(
    val repositionMode: RepositionMode = RepositionMode.CONTAIN,
    val scaleAlgorithm: ScaleAlgorithm = ScaleAlgorithm.BILINEAR,
    val ditherAlgorithm: DitherAlgorithm = DitherAlgorithm.FLOYD_STEINBERG,
    /** RGB 24-bit（0xRRGGBB）。 */
    val alphaBackgroundColorRgb: Int = 0xFFFFFF,
    /**
     * GIF 帧延迟下限（毫秒）。
     *
     * GIF 的原始延迟单位为 `1/100s`，换算后为 `delayCentiseconds * 10ms`。
     * 该值用于 clamp 过小/为 0 的 delay：
     *
     * `effectiveDelayMillis = max(delayCentiseconds * 10, minFrameDelayMillis)`
     */
    val minFrameDelayMillis: Int = 20,
    /**
     * 动图重采样的输出帧间隔（毫秒）。
     *
     * 动图最终会被重采样为“等间隔输出帧序列”（用 repeat 表达长 delay），该值决定输出帧的时间粒度。
     * 通常建议与 [minFrameDelayMillis] 相同。
     */
    val frameSampleIntervalMillis: Int = 20,
) {
    init {
        require(alphaBackgroundColorRgb in 0x000000..0xFFFFFF) {
            "alphaBackgroundColorRgb must be in [0x000000, 0xFFFFFF]"
        }
        require(minFrameDelayMillis > 0) { "minFrameDelayMillis must be > 0" }
        require(frameSampleIntervalMillis > 0) { "frameSampleIntervalMillis must be > 0" }
    }
}

enum class RepositionMode {
    COVER,
    CONTAIN,
    STRETCH,
}

enum class ScaleAlgorithm {
    BILINEAR,
}

enum class DitherAlgorithm {
    NONE,
    ORDERED_BAYER,
    FLOYD_STEINBERG,
}
