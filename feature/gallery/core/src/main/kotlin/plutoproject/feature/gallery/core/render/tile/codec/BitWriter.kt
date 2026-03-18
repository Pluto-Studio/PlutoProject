package plutoproject.feature.gallery.core.render.tile.codec

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
