package plutoproject.feature.gallery.core.render.tile

class TilePoolSnapshot(
    val offsets: IntArray,
    val blob: ByteArray,
)

class TilePool private constructor(
    private val offsets: IntArray,
    private val blob: ByteArray,
) : TilePoolView {
    init {
        require(offsets.isNotEmpty()) { "offsets must not be empty" }
        require(offsets[0] == 0) { "offsets must start with 0" }
        require(offsets.size <= MAX_TILE_POOL_UNIQUE_TILE_COUNT + 1) {
            "offsets size exceeds max tile capacity: size=${offsets.size}, max=${MAX_TILE_POOL_UNIQUE_TILE_COUNT + 1}"
        }

        var index = 1
        while (index < offsets.size) {
            require(offsets[index] >= offsets[index - 1]) {
                "offsets must be non-decreasing: offsets[$index]=${offsets[index]}, offsets[${index - 1}]=${offsets[index - 1]}"
            }
            index++
        }

        require(offsets.last() == blob.size) {
            "last offset must equal blob size: lastOffset=${offsets.last()}, blobSize=${blob.size}"
        }
    }

    override val tileCount: Int
        get() = offsets.size - 1

    override fun getTile(index: Int): TileDataView {
        require(index in 0 until tileCount) { "tile index out of range: $index" }
        return TileDataView(
            blob = blob,
            start = offsets[index],
            end = offsets[index + 1],
        )
    }

    fun snapshot(): TilePoolSnapshot = TilePoolSnapshot(
        offsets = offsets.copyOf(),
        blob = blob.copyOf(),
    )

    companion object {
        fun fromSnapshot(snapshot: TilePoolSnapshot): TilePool = TilePool(
            offsets = snapshot.offsets.copyOf(),
            blob = snapshot.blob.copyOf(),
        )
    }
}
