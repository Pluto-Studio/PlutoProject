package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.image.StaticImageData

/**
 * 静态图预渲染器。
 *
 * 约定：
 * - 输入已经是解码后的 [RgbaImage8888]；解码/IO 不属于 core renderer 的职责
 * - 建议实现为纯 CPU pipeline（不依赖 Paper/Velocity API）
 * - 需要线程/并发控制时，在 adapter 层用 `withContext(dispatcher)` 包装本 renderer（或包装 UseCase）
 */
fun interface StaticImageRenderer {
    suspend fun render(request: RenderStaticImageRequest): RenderResult<StaticImageData>
}

/**
 * 动图预渲染器。
 *
 * 约定同 [StaticImageRenderer]。
 */
fun interface AnimatedImageRenderer {
    suspend fun render(request: RenderAnimatedImageRequest): RenderResult<AnimatedImageData>
}
