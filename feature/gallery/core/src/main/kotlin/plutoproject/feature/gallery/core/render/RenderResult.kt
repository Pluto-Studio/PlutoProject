package plutoproject.feature.gallery.core.render

sealed interface RenderResult<out T> {
    data class Success<T>(val data: T) : RenderResult<T>
    data object TilePoolOverflow : RenderResult<Nothing>
    data object TileIndexCountOverflow : RenderResult<Nothing>
    data object DurationOverflow : RenderResult<Nothing>
    data object OutputFrameCountOverflow : RenderResult<Nothing>
}
