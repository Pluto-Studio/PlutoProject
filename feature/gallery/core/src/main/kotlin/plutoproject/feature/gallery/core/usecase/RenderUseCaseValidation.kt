package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.TilePool
import plutoproject.feature.gallery.core.render.RenderStatus

internal const val MAX_UNIQUE_TILE_COUNT = 65_536

internal data class TileCountValidationResult(
    val tileCount: Int?,
    val status: RenderStatus?,
)

internal fun validateTileCount(mapXBlocks: Int, mapYBlocks: Int): TileCountValidationResult {
    if (mapXBlocks <= 0 || mapYBlocks <= 0) {
        return TileCountValidationResult(
            tileCount = null,
            status = RenderStatus.INVALID_TILE_COUNT,
        )
    }

    val tileCountLong = mapXBlocks.toLong() * mapYBlocks.toLong()
    if (tileCountLong > Int.MAX_VALUE.toLong()) {
        return TileCountValidationResult(
            tileCount = null,
            status = RenderStatus.TILE_COUNT_OVERFLOW,
        )
    }

    return TileCountValidationResult(
        tileCount = tileCountLong.toInt(),
        status = null,
    )
}

internal fun uniqueTileCount(tilePool: TilePool): Int? {
    if (tilePool.offsets.isEmpty()) {
        return null
    }
    return tilePool.offsets.size - 1
}
