package plutoproject.feature.gallery.core.render.mapcolor

private const val RGB565_TABLE_SIZE = 1 shl 16

/**
 * 计算 RGB565 -> mapColor 的 O(1) 查询表。
 *
 * 约定：
 * - 距离度量为 RGB 空间平方欧氏距离
 * - 距离相等时，选择更小的 mapColor byte（保证可复现）
 */
fun calcRgb565ToMapColor(palette: MapColorPalette): ByteArray {
    val candidateMapColors = IntArray(palette.candidates.size) { index ->
        palette.candidates[index].toInt() and 0xFF
    }
    val rgbOfMapColor = palette.rgbOfMapColor

    val table = ByteArray(RGB565_TABLE_SIZE)
    for (rgb565 in 0 until RGB565_TABLE_SIZE) {
        val rgb24 = rgb565ToRgb24(rgb565)
        val red = (rgb24 ushr 16) and 0xFF
        val green = (rgb24 ushr 8) and 0xFF
        val blue = rgb24 and 0xFF

        var bestMapColor = candidateMapColors[0]
        var bestDistance = colorDistanceSquared(red, green, blue, rgbOfMapColor[bestMapColor])

        for (index in 1 until candidateMapColors.size) {
            val candidateMapColor = candidateMapColors[index]
            val candidateDistance = colorDistanceSquared(
                red,
                green,
                blue,
                rgbOfMapColor[candidateMapColor],
            )

            if (
                candidateDistance < bestDistance ||
                (candidateDistance == bestDistance && candidateMapColor < bestMapColor)
            ) {
                bestMapColor = candidateMapColor
                bestDistance = candidateDistance
            }
        }

        table[rgb565] = bestMapColor.toByte()
    }

    return table
}

/** RGB24（0xRRGGBB）压缩到 RGB565（0..65535）。 */
internal fun rgb24ToRgb565(rgb24: Int): Int {
    val red = (rgb24 ushr 16) and 0xFF
    val green = (rgb24 ushr 8) and 0xFF
    val blue = rgb24 and 0xFF

    // RGB565 位宽：R5 + G6 + B5。
    // 这里的量化用于“查表索引空间压缩”，不是最终显示格式。
    val red5 = red ushr 3
    val green6 = green ushr 2
    val blue5 = blue ushr 3

    return (red5 shl 11) or (green6 shl 5) or blue5
}

/** RGB565（0..65535）展开到 RGB24（0xRRGGBB）。 */
internal fun rgb565ToRgb24(rgb565: Int): Int {
    val red5 = (rgb565 ushr 11) and 0x1F
    val green6 = (rgb565 ushr 5) and 0x3F
    val blue5 = rgb565 and 0x1F

    val red = (red5 shl 3) or (red5 ushr 2)
    val green = (green6 shl 2) or (green6 ushr 4)
    val blue = (blue5 shl 3) or (blue5 ushr 2)

    return (red shl 16) or (green shl 8) or blue
}

private fun colorDistanceSquared(red: Int, green: Int, blue: Int, rgb24: Int): Int {
    val targetRed = (rgb24 ushr 16) and 0xFF
    val targetGreen = (rgb24 ushr 8) and 0xFF
    val targetBlue = rgb24 and 0xFF

    val dRed = red - targetRed
    val dGreen = green - targetGreen
    val dBlue = blue - targetBlue

    return dRed * dRed + dGreen * dGreen + dBlue * dBlue
}
