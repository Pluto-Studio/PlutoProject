package plutoproject.feature.gallery.core.render.tile.dedupe

internal fun interface TileMapColorHasher {
    fun hash(tileMapColors: ByteArray): Long
}

