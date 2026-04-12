package plutoproject.feature.gallery.core.render

/**
 * 代表渲染管线中的共享数据（如当前画面内容）。
 */
class RenderWorkspace(
    val width: Int,
    val height: Int,
    pixelBuffer: PixelBuffer
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require(pixelBuffer.width == width && pixelBuffer.height == height) { "Size must be equal to initial pixel buffer's size" }
    }

    var pixelBuffer: PixelBuffer? = pixelBuffer
    var transparentMask: TransparentMask? = null
    var mapColorPixelBuffer: MapColorPixelBuffer? = null

    fun writeMapColorPixels(pixels: ByteArray) {
        checkNotNull(pixelBuffer) { "pixelBuffer must not be null before writing map color pixels" }

        mapColorPixelBuffer = MapColorPixelBuffer(
            width = pixelBuffer!!.width,
            height = pixelBuffer!!.height,
            pixels = pixels,
        )

        // 写 Map Color 之后就用不上原本的 pixelBuffer 了，释放掉相关资源引用
        pixelBuffer = null
        transparentMask = null
    }
}
