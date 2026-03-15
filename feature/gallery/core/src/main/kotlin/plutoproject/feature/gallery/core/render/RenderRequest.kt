package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.decode.DecodedAnimatedImageSource

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
 * 动图渲染输入。
 *
 * [source] 需提供稳定的时间轴和可顺序消费的帧流。
 */
data class RenderAnimatedImageRequest(
    val source: DecodedAnimatedImageSource,
    val mapXBlocks: Int,
    val mapYBlocks: Int,
    val profile: RenderProfile,
)
