package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.render.tile.TILE_SIDE_PIXELS

class TargetResolution(val width: Int, val height: Int)

internal fun calcTargetResolution(widthBlocks: Int, heightBlocks: Int): TargetResolution {
    require(widthBlocks > 0) { "widthBlocks must be > 0" }
    require(heightBlocks > 0) { "heightBlocks must be > 0" }

    val widthLong = widthBlocks.toLong() * TILE_SIDE_PIXELS
    val heightLong = heightBlocks.toLong() * TILE_SIDE_PIXELS

    require(widthLong <= Int.MAX_VALUE.toLong()) {
        "target width overflow: mapXBlocks=$widthBlocks"
    }
    require(heightLong <= Int.MAX_VALUE.toLong()) {
        "target height overflow: mapYBlocks=$heightBlocks"
    }

    return TargetResolution(
        width = widthLong.toInt(),
        height = heightLong.toInt(),
    )
}
