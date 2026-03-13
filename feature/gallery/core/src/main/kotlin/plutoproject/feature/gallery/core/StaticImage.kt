package plutoproject.feature.gallery.core

import plutoproject.feature.gallery.core.util.isValidOwnerName
import java.util.*

/**
 * 代表一张静态图。
 *
 * @see Image
 */
class StaticImage(
    override val id: UUID,
    override val owner: UUID,
    private var _ownerName: String,
    private var _name: String,
    override val mapWidthBlocks: Int,
    override val mapHeightBlocks: Int,
    override val tileMapIds: IntArray,
    private var _imageData: StaticImageData,
) : Image<StaticImageData>() {
    override val ownerName: String
        get() = _ownerName
    override val name: String
        get() = _name
    override val imageData: StaticImageData
        get() = _imageData

    override fun changeOwnerName(name: String) {
        require(isValidOwnerName(name)) { "Invalid owner name: $name" }
        _ownerName = name
    }

    override fun rename(name: String) {
        // TODO: 名称规范检查
        _name = name
    }

    override fun replaceData(data: StaticImageData) {
        // TODO: 更新 data
    }
}
