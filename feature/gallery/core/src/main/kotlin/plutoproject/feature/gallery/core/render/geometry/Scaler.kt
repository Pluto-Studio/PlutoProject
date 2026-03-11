package plutoproject.feature.gallery.core.render.geometry

import plutoproject.feature.gallery.core.render.RgbaImage8888
import plutoproject.feature.gallery.core.render.ScaleAlgorithm

internal const val MAP_BLOCK_PIXEL_SIZE = 128

internal data class TargetResolution(
    val width: Int,
    val height: Int,
)

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
    fun scale(source: RgbaImage8888, transform: DestToSourceTransform): RgbaImage8888
}

internal fun scalerOf(algorithm: ScaleAlgorithm): Scaler = when (algorithm) {
    ScaleAlgorithm.BILINEAR -> BilinearScaler
    ScaleAlgorithm.LANCZOS -> BilinearScaler
}
