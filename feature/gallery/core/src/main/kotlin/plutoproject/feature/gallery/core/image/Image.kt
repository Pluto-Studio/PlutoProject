package plutoproject.feature.gallery.core.image

import java.util.*

private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,16}$")

/**
 * 地图画。
 */
class Image(
    /**
     * 地图画的 ID，在整个系统中唯一。
     */
    val id: UUID,

    /**
     * 地图画类型。
     * @see ImageType
     */
    val type: ImageType,

    /**
     * 创建此地图画的玩家 UUID。
     */
    val owner: UUID,

    /**
     * 创建此地图画的玩家名称，发现玩家名称修改后会同步。
     */
    ownerName: String,

    /**
     * 地图画的名称。
     */
    name: String,

    /**
     * 地图宽度方块数。
     *
     * 实际宽像素量为 [mapWidthBlocks] * 128。
     */
    val mapWidthBlocks: Int,

    /**
     * 地图高度方块数。
     *
     * 实际高像素量为 [mapHeightBlocks] * 128。
     */
    val mapHeightBlocks: Int,

    /**
     * 为这个地图所有分区分配的 Map ID，每个 Map ID 在系统中都是独一无二的。
     *
     * 每 128*128 的分区会被分为一个块，按照从左到右、从上到下的顺序陈列在数组内。
     */
    val tileMapIds: IntArray,
) {
    var ownerName = ownerName
        private set

    var name = name
        private set

    internal fun changeOwnerName(name: String) {
        require(USERNAME_REGEX.matches(name)) { "Invalid owner username: $name" }
        ownerName = name
    }

    internal fun rename(name: String) {
        // TODO: 名称规范检查
        this.name = name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Image
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
