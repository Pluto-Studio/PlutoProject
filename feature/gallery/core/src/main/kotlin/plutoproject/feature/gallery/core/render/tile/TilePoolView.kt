package plutoproject.feature.gallery.core.render.tile

/**
 * 多个已编码 Tile 数据的只读集合视图。
 */
interface TilePoolView {
    val tileCount: Int

    fun getTile(index: Int): TileDataView
}
