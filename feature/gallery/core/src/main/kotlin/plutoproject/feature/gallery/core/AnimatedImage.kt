package plutoproject.feature.gallery.core

import java.util.*

/**
 * 代表一张动图。
 *
 * @see Image
 */
class AnimatedImage(
    override val id: UUID,
    override val owner: UUID,
    private var _ownerName: String,
    private var _name: String,
    override val mapWidthBlocks: Int,
    override val mapHeightBlocks: Int,
    override val tileMapIds: IntArray,
    private var _imageData: AnimatedImageData
) : Image<AnimatedImageData>() {
    override val ownerName: String
        get() = _ownerName
    override val name: String
        get() = _name
    override val imageData: AnimatedImageData
        get() = _imageData

    override fun changeOwnerName(name: String) {
        _ownerName = name
    }

    override fun rename(name: String) {
        // TODO: 名称规范检查
        _name = name
    }

    override fun replaceData(data: AnimatedImageData) {
        // TODO: 更新 data
    }
}
