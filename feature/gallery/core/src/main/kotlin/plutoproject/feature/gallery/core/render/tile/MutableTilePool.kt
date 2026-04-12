package plutoproject.feature.gallery.core.render.tile

class MutableTilePool(
    initialTileCapacity: Int = 16,
    initialBlobCapacityBytes: Int = 4096,
) {
    init {
        require(initialTileCapacity in 1..MAX_TILE_POOL_UNIQUE_TILE_COUNT) {
            "initialTileCapacity must be in [1, $MAX_TILE_POOL_UNIQUE_TILE_COUNT], actual=$initialTileCapacity"
        }
    }

    private var offsets = IntArray(initialTileCapacity.coerceAtLeast(1) + 1)
    private var tileCount = 0

    private var blob = ByteArray(initialBlobCapacityBytes.coerceAtLeast(1))
    private var blobSize = 0

    init {
        offsets[0] = 0
    }

    val size: Int
        get() = tileCount

    fun addTile(tileData: ByteArray): Int {
        require(tileCount < MAX_TILE_POOL_UNIQUE_TILE_COUNT) {
            "tile count overflow: max=$MAX_TILE_POOL_UNIQUE_TILE_COUNT"
        }

        val tileIndex = tileCount

        ensureBlobCapacity(tileData.size)
        tileData.copyInto(blob, destinationOffset = blobSize)
        blobSize += tileData.size

        tileCount++
        ensureOffsetsCapacity(tileCount + 1)
        offsets[tileCount] = blobSize
        return tileIndex
    }

    fun encodedTileEquals(tileIndex: Int, expectedTileData: ByteArray): Boolean {
        require(tileIndex in 0 until tileCount) {
            "tileIndex out of range: index=$tileIndex, tileCount=$tileCount"
        }

        val start = offsets[tileIndex]
        val end = offsets[tileIndex + 1]
        val actualSize = end - start
        if (actualSize != expectedTileData.size) {
            return false
        }

        for (offset in expectedTileData.indices) {
            if (blob[start + offset] != expectedTileData[offset]) {
                return false
            }
        }
        return true
    }

    fun freeze(): TilePool = TilePool.fromSnapshot(
        TilePoolSnapshot(
            offsets = offsets.copyOf(tileCount + 1),
            blob = blob.copyOf(blobSize),
        )
    )

    private fun ensureOffsetsCapacity(requiredSize: Int) {
        if (requiredSize <= offsets.size) {
            return
        }

        var newSize = offsets.size
        while (newSize < requiredSize) {
            newSize = newSize shl 1
        }
        offsets = offsets.copyOf(newSize)
    }

    private fun ensureBlobCapacity(additionalBytes: Int) {
        if (additionalBytes == 0) {
            return
        }

        val requiredSizeLong = blobSize.toLong() + additionalBytes.toLong()
        require(requiredSizeLong <= Int.MAX_VALUE.toLong()) {
            "tile blob size overflow: required=$requiredSizeLong"
        }
        val requiredSize = requiredSizeLong.toInt()

        if (requiredSize <= blob.size) {
            return
        }

        var newSize = blob.size.toLong()
        while (newSize < requiredSizeLong) {
            newSize = newSize shl 1
            if (newSize > Int.MAX_VALUE.toLong()) {
                newSize = Int.MAX_VALUE.toLong()
                break
            }
        }
        blob = blob.copyOf(newSize.toInt())
    }
}
