package plutoproject.feature.gallery.core.render.geometry

import plutoproject.feature.gallery.core.render.RgbaImage8888
import plutoproject.feature.gallery.core.render.ScaleAlgorithm

internal const val MAP_BLOCK_PIXEL_SIZE = 128

internal data class TargetResolution(
    val width: Int,
    val height: Int,
)

/**
 * 目标渲染分辨率：每个地图方块固定 128x128 像素。
 */
internal fun calcTargetResolution(mapXBlocks: Int, mapYBlocks: Int): TargetResolution {
    require(mapXBlocks > 0) { "mapXBlocks must be > 0" }
    require(mapYBlocks > 0) { "mapYBlocks must be > 0" }

    val widthLong = mapXBlocks.toLong() * MAP_BLOCK_PIXEL_SIZE
    val heightLong = mapYBlocks.toLong() * MAP_BLOCK_PIXEL_SIZE
    require(widthLong <= Int.MAX_VALUE.toLong()) {
        "target width overflow: mapXBlocks=$mapXBlocks"
    }
    require(heightLong <= Int.MAX_VALUE.toLong()) {
        "target height overflow: mapYBlocks=$mapYBlocks"
    }

    return TargetResolution(
        width = widthLong.toInt(),
        height = heightLong.toInt(),
    )
}

internal fun interface Scaler {
    /**
     * 按 dest->src 变换生成目标 RGBA 图。
     */
    fun scale(source: RgbaImage8888, transform: DestToSourceTransform): RgbaImage8888
}

internal fun scalerOf(algorithm: ScaleAlgorithm): Scaler = when (algorithm) {
    ScaleAlgorithm.BILINEAR -> BilinearScaler
    ScaleAlgorithm.LANCZOS -> TODO("Lanczos scaler is not implemented yet")
}
