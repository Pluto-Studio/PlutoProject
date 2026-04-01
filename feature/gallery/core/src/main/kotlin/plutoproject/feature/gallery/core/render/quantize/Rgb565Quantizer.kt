package plutoproject.feature.gallery.core.render.quantize

private const val RGB565_TABLE_SIZE = 1 shl 16

object Rgb565Quantizer : Quantizer {
    private val rgb565ToMapColor by lazy { buildRgb565ToMapColor() }

    override fun quantize(rgb24: Int): Byte {
        val rgb565 = rgb24ToRgb565(rgb24)
        return rgb565ToMapColor[rgb565]
    }
}

private fun buildRgb565ToMapColor(): ByteArray {
    val table = ByteArray(RGB565_TABLE_SIZE)

    for (rgb565 in 0 until RGB565_TABLE_SIZE) {
        val rgb24 = rgb565ToRgb24(rgb565)
        table[rgb565] = findNearestMapColor(rgb24)
    }

    return table
}
