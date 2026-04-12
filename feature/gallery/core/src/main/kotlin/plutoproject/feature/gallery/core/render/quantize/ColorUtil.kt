package plutoproject.feature.gallery.core.render.quantize

private const val FIRST_NON_TRANSPARENT_MAP_COLOR = 4
private const val LAST_NON_TRANSPARENT_MAP_COLOR = 247

private val nonTransparentMapColors = IntArray(
    LAST_NON_TRANSPARENT_MAP_COLOR - FIRST_NON_TRANSPARENT_MAP_COLOR + 1,
) { index ->
    FIRST_NON_TRANSPARENT_MAP_COLOR + index
}

internal fun rgb24ToRgb565(rgb24: Int): Int {
    val red = (rgb24 ushr 16) and 0xFF
    val green = (rgb24 ushr 8) and 0xFF
    val blue = rgb24 and 0xFF

    val red5 = red ushr 3
    val green6 = green ushr 2
    val blue5 = blue ushr 3

    return (red5 shl 11) or (green6 shl 5) or blue5
}

internal fun rgb565ToRgb24(rgb565: Int): Int {
    val red5 = (rgb565 ushr 11) and 0x1F
    val green6 = (rgb565 ushr 5) and 0x3F
    val blue5 = rgb565 and 0x1F

    val red = (red5 shl 3) or (red5 ushr 2)
    val green = (green6 shl 2) or (green6 ushr 4)
    val blue = (blue5 shl 3) or (blue5 ushr 2)

    return (red shl 16) or (green shl 8) or blue
}

internal fun colorDistanceSquared(aRgb24: Int, bRgb24: Int): Int {
    val aRed = (aRgb24 ushr 16) and 0xFF
    val aGreen = (aRgb24 ushr 8) and 0xFF
    val aBlue = aRgb24 and 0xFF

    val bRed = (bRgb24 ushr 16) and 0xFF
    val bGreen = (bRgb24 ushr 8) and 0xFF
    val bBlue = bRgb24 and 0xFF

    val diffRed = aRed - bRed
    val diffGreen = aGreen - bGreen
    val diffBlue = aBlue - bBlue

    return diffRed * diffRed + diffGreen * diffGreen + diffBlue * diffBlue
}

internal fun findNearestMapColor(rgb24: Int): Byte {
    var bestMapColor = nonTransparentMapColors[0]
    var bestDistance = colorDistanceSquared(rgb24, MapColorPalette.rgb24OfMapColor(bestMapColor.toByte()))

    var candidateIndex = 1
    while (candidateIndex < nonTransparentMapColors.size) {
        val candidateMapColor = nonTransparentMapColors[candidateIndex]
        val candidateDistance =
            colorDistanceSquared(rgb24, MapColorPalette.rgb24OfMapColor(candidateMapColor.toByte()))
        if (candidateDistance < bestDistance ||
            (candidateDistance == bestDistance && candidateMapColor < bestMapColor)
        ) {
            bestMapColor = candidateMapColor
            bestDistance = candidateDistance
        }

        candidateIndex++
    }

    return bestMapColor.toByte()
}
