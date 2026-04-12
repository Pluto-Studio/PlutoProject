package plutoproject.feature.gallery.core.render.quantize

/**
 * 代表一个用于将 24-bit RGB 颜色量化到地图颜色的量化器。
 */
interface Quantizer {
    fun quantize(rgb24: Int): Byte
}
