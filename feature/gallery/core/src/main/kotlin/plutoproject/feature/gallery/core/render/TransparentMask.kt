package plutoproject.feature.gallery.core.render

import java.util.*

class TransparentMask(width: Int, height: Int) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
    }

    private val pixelCount = width * height
    private val maskBitSet = BitSet(pixelCount)

    fun isTransparent(index: Int): Boolean {
        require(index in 0..<pixelCount) { "Pixel index out of bounds" }
        return maskBitSet[index]
    }

    fun setTransparent(index: Int) {
        require(index in 0..<pixelCount) { "Pixel index out of bounds" }
        maskBitSet.set(index)
    }
}
