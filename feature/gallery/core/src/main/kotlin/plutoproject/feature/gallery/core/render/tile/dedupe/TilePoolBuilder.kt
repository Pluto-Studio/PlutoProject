package plutoproject.feature.gallery.core.render.tile.dedupe

import plutoproject.feature.gallery.core.image.TilePool

/**
 * 逐个追加 tile bytes，并在最后构建 [plutoproject.feature.gallery.core.image.TilePool]。
 */
internal class TilePoolBuilder(
    initialTileCapacity: Int = 16,
    initialBlobCapacityBytes: Int = 4096,
) {
    private var offsets = IntArray(initialTileCapacity.coerceAtLeast(1) + 1)
    private var tileCount = 0

    private var blob = ByteArray(initialBlobCapacityBytes.coerceAtLeast(1))
    private var blobSize = 0

    init {
        offsets[0] = 0
    }

    val uniqueTileCount: Int
        get() = tileCount

    /**
     * 追加一个 tile bytes，返回其在 TilePool 中的 index。
     */
    fun appendTile(tileData: ByteArray): Int {
        val tilePoolIndex = tileCount

        ensureBlobCapacity(tileData.size)
        tileData.copyInto(blob, destinationOffset = blobSize)
        blobSize += tileData.size

        tileCount += 1
        ensureOffsetsCapacity(tileCount + 1)
        offsets[tileCount] = blobSize

        return tilePoolIndex
    }

    /**
     * 比较 TilePool 中某个 tile 与给定 tile bytes 是否字节级一致。
     */
    fun tileDataEquals(tileIndex: Int, expectedTileData: ByteArray): Boolean {
        require(tileIndex in 0 until tileCount) {
            "tileIndex out of range: index=$tileIndex, tileCount=$tileCount"
        }

        val start = offsets[tileIndex]
        val end = offsets[tileIndex + 1]
        val actualLength = end - start
        if (actualLength != expectedTileData.size) {
            return false
        }

        for (i in expectedTileData.indices) {
            if (blob[start + i] != expectedTileData[i]) {
                return false
            }
        }
        return true
    }

    fun build(): TilePool = TilePool(
        offsets = offsets.copyOf(tileCount + 1),
        blob = blob.copyOf(blobSize),
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
