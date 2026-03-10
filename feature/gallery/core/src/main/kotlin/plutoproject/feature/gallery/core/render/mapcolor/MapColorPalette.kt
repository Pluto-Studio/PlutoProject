package plutoproject.feature.gallery.core.render.mapcolor

private const val MAP_COLOR_TABLE_SIZE = 256
private const val FIRST_NON_TRANSPARENT_MAP_COLOR = 4
private const val LAST_NON_TRANSPARENT_MAP_COLOR = 247
private const val MODIFIER_COUNT = 4
private const val MAX_BASE_COLOR_ID = 61
private const val BRIGHTNESS_DENOMINATOR = 255

private val BRIGHTNESS_NUMERATORS = intArrayOf(
    180,
    220,
    255,
    135,
)

/**
 * Minecraft 地图色板。
 *
 * - [candidates]：用于“最近色匹配”的 mapColor 候选集合
 * - [rgbOfMapColor]：按 mapColor byte（0..255）索引得到对应 RGB24（0xRRGGBB）
 */
data class MapColorPalette(
    val candidates: ByteArray,
    val rgbOfMapColor: IntArray,
) {
    init {
        require(candidates.isNotEmpty()) { "candidates must not be empty" }
        require(rgbOfMapColor.size == MAP_COLOR_TABLE_SIZE) {
            "rgbOfMapColor size must be $MAP_COLOR_TABLE_SIZE"
        }
    }

    companion object {
        /**
         * 默认 vanilla 地图色板。
         *
         * 规则：
         * - 非透明候选为 `4..247`（`1..3` 为透明别名，不参与非透明匹配）
         * - 每个 base color 仅内置 HIGH（M2）颜色，再用固定亮度系数推导 M0/M1/M3
         */
        fun vanilla(): MapColorPalette = MapColorPalette(
            candidates = defaultCandidates(),
            rgbOfMapColor = buildVanillaRgbOfMapColor(),
        )
    }
}

private fun defaultCandidates(): ByteArray {
    val candidateCount = LAST_NON_TRANSPARENT_MAP_COLOR - FIRST_NON_TRANSPARENT_MAP_COLOR + 1
    return ByteArray(candidateCount) { index ->
        (FIRST_NON_TRANSPARENT_MAP_COLOR + index).toByte()
    }
}

private fun buildVanillaRgbOfMapColor(): IntArray {
    check(VANILLA_BASE_HIGH_RGB.size == MAX_BASE_COLOR_ID + 1) {
        "unexpected VANILLA_BASE_HIGH_RGB size=${VANILLA_BASE_HIGH_RGB.size}"
    }

    val rgbOfMapColor = IntArray(MAP_COLOR_TABLE_SIZE)
    for (baseColor in VANILLA_BASE_HIGH_RGB.indices) {
        val highRgb = VANILLA_BASE_HIGH_RGB[baseColor]
        val highRed = (highRgb ushr 16) and 0xFF
        val highGreen = (highRgb ushr 8) and 0xFF
        val highBlue = highRgb and 0xFF

        for (modifier in 0 until MODIFIER_COUNT) {
            val mapColor = (baseColor shl 2) or modifier
            val brightness = BRIGHTNESS_NUMERATORS[modifier]
            val red = scaleBrightness(highRed, brightness)
            val green = scaleBrightness(highGreen, brightness)
            val blue = scaleBrightness(highBlue, brightness)
            rgbOfMapColor[mapColor] = (red shl 16) or (green shl 8) or blue
        }
    }

    return rgbOfMapColor
}

private fun scaleBrightness(channel: Int, brightness: Int): Int {
    return channel * brightness / BRIGHTNESS_DENOMINATOR
}

private val VANILLA_BASE_HIGH_RGB = intArrayOf(
    0x000000, // base 0
    0x7FB238, // base 1
    0xF7E9A3, // base 2
    0xC7C7C7, // base 3
    0xFF0000, // base 4
    0xA0A0FF, // base 5
    0xA7A7A7, // base 6
    0x007C00, // base 7
    0xFFFFFF, // base 8
    0xA4A8B8, // base 9
    0x976D4D, // base 10
    0x707070, // base 11
    0x4040FF, // base 12
    0x8F7748, // base 13
    0xFFFCF5, // base 14
    0xD87F33, // base 15
    0xB24CD8, // base 16
    0x6699D8, // base 17
    0xE5E533, // base 18
    0x7FCC19, // base 19
    0xF27FA5, // base 20
    0x4C4C4C, // base 21
    0x999999, // base 22
    0x4C7F99, // base 23
    0x7F3FB2, // base 24
    0x334CB2, // base 25
    0x664C33, // base 26
    0x667F33, // base 27
    0x993333, // base 28
    0x191919, // base 29
    0xFAEE4D, // base 30
    0x5CDBD5, // base 31
    0x4A80FF, // base 32
    0x00D93A, // base 33
    0x815631, // base 34
    0x700200, // base 35
    0xD1B1A1, // base 36
    0x9F5224, // base 37
    0x95576C, // base 38
    0x706C8A, // base 39
    0xBA8524, // base 40
    0x677535, // base 41
    0xA04D4E, // base 42
    0x392923, // base 43
    0x876B62, // base 44
    0x575C5C, // base 45
    0x7A4958, // base 46
    0x4C3E5C, // base 47
    0x4C3223, // base 48
    0x4C522A, // base 49
    0x8E3C2E, // base 50
    0x251610, // base 51
    0xBD3031, // base 52
    0x943F61, // base 53
    0x5C191D, // base 54
    0x167E86, // base 55
    0x3A8E8C, // base 56
    0x562C3E, // base 57
    0x14B485, // base 58
    0x646464, // base 59
    0xD8AF93, // base 60
    0x7FA796, // base 61
)
