package plutoproject.feature.gallery.core.render.tile.codec

internal class BitWriter(initialCapacityBytes: Int) {
    private var bytes = ByteArray(initialCapacityBytes.coerceAtLeast(1))
    private var bitSize = 0

    fun writeBits(value: Int, bitCount: Int) {
        require(bitCount >= 0) { "bitCount must be >= 0" }
        if (bitCount == 0) {
            return
        }

        var remainingBits = bitCount
        while (remainingBits > 0) {
            ensureCapacity(bitSize + 1)

            val bitIndexFromMsb = remainingBits - 1
            val bit = (value ushr bitIndexFromMsb) and 1
            val byteIndex = bitSize ushr 3
            val bitIndexInByte = 7 - (bitSize and 7)
            if (bit == 1) {
                bytes[byteIndex] = (bytes[byteIndex].toInt() or (1 shl bitIndexInByte)).toByte()
            }
            bitSize++
            remainingBits--
        }
    }

    fun toByteArray(): ByteArray {
        val byteSize = (bitSize + 7) ushr 3
        return bytes.copyOf(byteSize)
    }

    private fun ensureCapacity(requiredBits: Int) {
        val requiredBytes = (requiredBits + 7) ushr 3
        if (requiredBytes <= bytes.size) {
            return
        }

        var newSize = bytes.size
        while (newSize < requiredBytes) {
            newSize = newSize shl 1
        }
        bytes = bytes.copyOf(newSize)
    }
}
