package plutoproject.feature.gallery.core.render.tile.dedupe

sealed interface TileDedupeResult {
    data class Success(val index: Int) : TileDedupeResult
    data object TilePoolOverflow : TileDedupeResult
}
