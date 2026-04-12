package plutoproject.feature.gallery.core.render.quantize

object NearestColorQuantizer : Quantizer {
    override fun quantize(rgb24: Int): Byte {
        return findNearestMapColor(rgb24)
    }
}
