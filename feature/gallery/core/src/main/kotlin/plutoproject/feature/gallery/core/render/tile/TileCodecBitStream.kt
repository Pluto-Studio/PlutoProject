package plutoproject.feature.gallery.core.render.tile

internal class BitWriter(initialCapacityBytes: Int = 256) {
    private var buffer = ByteArray(initialCapacityBytes)
    private var writtenBits = 0

    fun writeBits(value: Int, bitCount: Int) {
        require(bitCount in 0..31) { "bitCount must be in [0, 31], actual=$bitCount" }
        if (bitCount == 0) {
            return
        }

        require((value ushr bitCount) == 0) {
            "value has non-zero bits outside bitCount: value=$value, bitCount=$bitCount"
        }

        ensureCapacity(bitCount)
        for (shift in bitCount - 1 downTo 0) {
            val bit = (value ushr shift) and 1
            if (bit == 1) {
                val byteIndex = writtenBits ushr 3
                val bitIndex = 7 - (writtenBits and 7)
                buffer[byteIndex] = (buffer[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
            writtenBits++
        }
    }

    fun toByteArray(): ByteArray {
        val bytes = (writtenBits + 7) ushr 3
        return buffer.copyOf(bytes)
    }

    private fun ensureCapacity(additionalBits: Int) {
        val requiredBits = writtenBits + additionalBits
        val requiredBytes = (requiredBits + 7) ushr 3
        if (requiredBytes <= buffer.size) {
            return
        }

        var newSize = buffer.size
        while (newSize < requiredBytes) {
            newSize = newSize shl 1
        }
        buffer = buffer.copyOf(newSize)
    }
}

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
