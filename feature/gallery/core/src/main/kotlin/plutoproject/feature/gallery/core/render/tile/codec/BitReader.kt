package plutoproject.feature.gallery.core.render.tile.codec

internal class BitReader(
    private val bytes: ByteArray,
    private val startOffset: Int,
    private val length: Int,
) {
    private var readBits = 0
    private val totalBits = length * 8

    init {
        require(startOffset >= 0) { "startOffset must be >= 0" }
        require(length >= 0) { "length must be >= 0" }
        require(startOffset + length <= bytes.size) {
            "invalid range: startOffset=$startOffset, length=$length, bytes.size=${bytes.size}"
        }
    }

    fun remainingBits(): Int = totalBits - readBits

    fun readBits(bitCount: Int): Int {
        require(bitCount in 0..31) { "bitCount must be in [0, 31], actual=$bitCount" }
        if (bitCount == 0) {
            return 0
        }

        require(remainingBits() >= bitCount) {
            "not enough bits: requested=$bitCount, remaining=${remainingBits()}"
        }

        var value = 0
        repeat(bitCount) {
            val absoluteBitIndex = readBits
            val byteIndex = startOffset + (absoluteBitIndex ushr 3)
            val bitIndex = 7 - (absoluteBitIndex and 7)
            val bit = (bytes[byteIndex].toInt() ushr bitIndex) and 1
            value = (value shl 1) or bit
            readBits++
        }

        return value
    }
}
