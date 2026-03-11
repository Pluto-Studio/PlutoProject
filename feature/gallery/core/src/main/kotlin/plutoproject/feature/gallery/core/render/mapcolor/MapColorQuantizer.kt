package plutoproject.feature.gallery.core.render.mapcolor

import plutoproject.feature.gallery.core.render.DitherAlgorithm

private const val RGB565_TABLE_SIZE = 1 shl 16
private const val ORDERED_DITHER_STRENGTH = 16

private val ORDERED_BAYER_4X4 = intArrayOf(
    0, 8, 2, 10,
    12, 4, 14, 6,
    3, 11, 1, 9,
    15, 7, 13, 5,
)

internal fun interface MapColorQuantizer {
    /**
     * 将 AlphaCompositor 产物量化为 mapColor bytes。
     *
     * 输出约束：
     * - 透明像素只输出 `0`
     * - 非透明像素只输出 `4..247`（不输出 `1..3` 透明别名）
     */
    fun quantize(composited: CompositedRgbImage, ditherAlgorithm: DitherAlgorithm): ByteArray
}

/**
 * MapColor 量化默认实现。
 *
 * 设计意图：
 * - 抖动/误差扩散在 RGB24 空间进行（更符合算法语义）
 * - 最终映射到 mapColor 时，先 `RGB24 -> RGB565`，再查 `rgb565ToMapColor`
 *   以 `ByteArray(65536)` 达到 O(1) 查询和更低运行时开销
 */
internal class DefaultMapColorQuantizer(
    private val palette: MapColorPalette,
    private val rgb565ToMapColor: ByteArray,
) : MapColorQuantizer {
    init {
        require(rgb565ToMapColor.size == RGB565_TABLE_SIZE) {
            "rgb565ToMapColor size must be $RGB565_TABLE_SIZE"
        }
    }

    override fun quantize(composited: CompositedRgbImage, ditherAlgorithm: DitherAlgorithm): ByteArray {
        return when (ditherAlgorithm) {
            DitherAlgorithm.NONE -> quantizeWithoutDither(composited)
            DitherAlgorithm.ORDERED_BAYER -> quantizeOrderedBayer(composited)
            DitherAlgorithm.FLOYD_STEINBERG -> quantizeFloydSteinberg(composited)
        }
    }

    private fun quantizeWithoutDither(composited: CompositedRgbImage): ByteArray {
        val result = ByteArray(composited.rgb24Pixels.size)
        var index = 0
        while (index < composited.rgb24Pixels.size) {
            if (composited.transparentMask[index]) {
                result[index] = 0
                index++
                continue
            }

            val rgb24 = composited.rgb24Pixels[index]
            result[index] = mapColorOfRgb24(rgb24)
            index++
        }
        return result
    }

    private fun quantizeOrderedBayer(composited: CompositedRgbImage): ByteArray {
        val result = ByteArray(composited.rgb24Pixels.size)
        val width = composited.width

        var index = 0
        var y = 0
        while (y < composited.height) {
            var x = 0
            while (x < width) {
                if (composited.transparentMask[index]) {
                    result[index] = 0
                    x++
                    index++
                    continue
                }

                val rgb24 = composited.rgb24Pixels[index]
                val ditherOffset = orderedOffsetAt(x, y)

                val red = ((rgb24 ushr 16) and 0xFF) + ditherOffset
                val green = ((rgb24 ushr 8) and 0xFF) + ditherOffset
                val blue = (rgb24 and 0xFF) + ditherOffset

                val adjustedRgb24 = (clampToByte(red) shl 16) or (clampToByte(green) shl 8) or clampToByte(blue)
                result[index] = mapColorOfRgb24(adjustedRgb24)

                x++
                index++
            }
            y++
        }

        return result
    }

    private fun quantizeFloydSteinberg(composited: CompositedRgbImage): ByteArray {
        val width = composited.width
        val height = composited.height
        val result = ByteArray(composited.rgb24Pixels.size)

        var currentRowErrorRed = IntArray(width + 2)
        var currentRowErrorGreen = IntArray(width + 2)
        var currentRowErrorBlue = IntArray(width + 2)
        var nextRowErrorRed = IntArray(width + 2)
        var nextRowErrorGreen = IntArray(width + 2)
        var nextRowErrorBlue = IntArray(width + 2)

        var index = 0
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (composited.transparentMask[index]) {
                    result[index] = 0
                    x++
                    index++
                    continue
                }

                val rgb24 = composited.rgb24Pixels[index]
                val red = clampToByte(((rgb24 ushr 16) and 0xFF) + roundFixed16(currentRowErrorRed[x + 1]))
                val green = clampToByte(((rgb24 ushr 8) and 0xFF) + roundFixed16(currentRowErrorGreen[x + 1]))
                val blue = clampToByte((rgb24 and 0xFF) + roundFixed16(currentRowErrorBlue[x + 1]))
                val adjustedRgb24 = (red shl 16) or (green shl 8) or blue

                val mapColor = mapColorOfRgb24(adjustedRgb24)
                result[index] = mapColor

                val mappedRgb24 = palette.rgbOfMapColor[mapColor.toInt() and 0xFF]
                val redError = red - ((mappedRgb24 ushr 16) and 0xFF)
                val greenError = green - ((mappedRgb24 ushr 8) and 0xFF)
                val blueError = blue - (mappedRgb24 and 0xFF)

                diffuseError(currentRowErrorRed, nextRowErrorRed, x, redError)
                diffuseError(currentRowErrorGreen, nextRowErrorGreen, x, greenError)
                diffuseError(currentRowErrorBlue, nextRowErrorBlue, x, blueError)

                x++
                index++
            }

            val swapRed = currentRowErrorRed
            currentRowErrorRed = nextRowErrorRed
            nextRowErrorRed = swapRed
            nextRowErrorRed.fill(0)

            val swapGreen = currentRowErrorGreen
            currentRowErrorGreen = nextRowErrorGreen
            nextRowErrorGreen = swapGreen
            nextRowErrorGreen.fill(0)

            val swapBlue = currentRowErrorBlue
            currentRowErrorBlue = nextRowErrorBlue
            nextRowErrorBlue = swapBlue
            nextRowErrorBlue.fill(0)

            y++
        }

        return result
    }

    private fun mapColorOfRgb24(rgb24: Int): Byte {
        // RGB24 颜色空间（2^24）过大，不适合直接建表；
        // 先压缩到 RGB565（2^16）后再查表，是性能与精度的折中。
        val rgb565 = rgb24ToRgb565(rgb24)
        return rgb565ToMapColor[rgb565]
    }
}

internal fun newDefaultMapColorQuantizer(): DefaultMapColorQuantizer {
    val palette = MapColorPalette.vanilla()
    val table = calcRgb565ToMapColor(palette)
    return DefaultMapColorQuantizer(palette, table)
}

private fun orderedOffsetAt(x: Int, y: Int): Int {
    val matrixValue = ORDERED_BAYER_4X4[(y and 3) * 4 + (x and 3)]
    return (matrixValue - 8) * ORDERED_DITHER_STRENGTH / 16
}

private fun clampToByte(value: Int): Int = value.coerceIn(0, 255)

private fun roundFixed16(fixed16Value: Int): Int {
    return if (fixed16Value >= 0) {
        (fixed16Value + 8) shr 4
    } else {
        (fixed16Value - 8) shr 4
    }
}

private fun diffuseError(currentRowError: IntArray, nextRowError: IntArray, x: Int, error: Int) {
    currentRowError[x + 2] += error * 7
    nextRowError[x] += error * 3
    nextRowError[x + 1] += error * 5
    nextRowError[x + 2] += error
}
