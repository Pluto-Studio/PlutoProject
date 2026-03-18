package plutoproject.feature.gallery.core

class MapUpdate(
    val mapId: Int,
    val mapColors: ByteArray,
) {
    init {
        require(mapColors.size == MAP_UPDATE_PIXEL_COUNT) {
            "mapColors size must be $MAP_UPDATE_PIXEL_COUNT, actual=${mapColors.size}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MapUpdate
        if (mapId != other.mapId) return false
        return mapColors.contentEquals(other.mapColors)
    }

    override fun hashCode(): Int {
        var result = mapId
        result = 31 * result + mapColors.contentHashCode()
        return result
    }

    companion object {
        const val MAP_UPDATE_PIXEL_COUNT: Int = 128 * 128
    }
}
