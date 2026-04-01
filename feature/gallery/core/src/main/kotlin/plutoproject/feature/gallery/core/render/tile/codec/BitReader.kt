package plutoproject.feature.gallery.core.render.tile.codec

internal class BitReader(
    private val source: ByteArray,
    startOffset: Int,
    length: Int,
) {
    private val endOffset = startOffset + length
    private var bitOffset = startOffset * 8

    init {
        require(startOffset >= 0) { "startOffset must be >= 0" }
        require(length >= 0) { "length must be >= 0" }
        require(endOffset <= source.size) {
            "bit reader range out of bounds: endOffset=$endOffset, sourceSize=${source.size}"
        }
    }

    fun readBits(bitCount: Int): Int {
        require(bitCount >= 0) { "bitCount must be >= 0" }
        require(remainingBits() >= bitCount) {
            "not enough bits left: requested=$bitCount, remaining=${remainingBits()}"
        }

        var result = 0
        repeat(bitCount) {
            val byteIndex = bitOffset ushr 3
            val bitIndexInByte = 7 - (bitOffset and 7)
            val bit = (source[byteIndex].toInt() ushr bitIndexInByte) and 1
            result = (result shl 1) or bit
            bitOffset++
        }
        return result
    }

    fun remainingBits(): Int = endOffset * 8 - bitOffset
}
