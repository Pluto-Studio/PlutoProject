package plutoproject.feature.gallery.core

import java.util.*

/**
 * 地图画基类，包含动图和静态图的共享属性。
 */
abstract class Image<T> {
    /**
     * 地图画的 ID，在整个系统中唯一。
     */
    abstract val id: UUID

    /**
     * 创建此地图画的玩家 UUID。
     */
    abstract val owner: UUID

    /**
     * 创建此地图画的玩家名称，发现玩家名称修改后会同步。
     */
    abstract val ownerName: String

    /**
     * 地图画的名称。
     */
    abstract val name: String

    /**
     * 地图宽度方块数。
     *
     * 实际宽像素量为 [mapWidthBlocks] * 128。
     */
    abstract val mapWidthBlocks: Int

    /**
     * 地图高度方块数。
     *
     * 实际高像素量为 [mapHeightBlocks] * 128。
     */
    abstract val mapHeightBlocks: Int

    /**
     * 为这个地图所有分区分配的 Map ID，每个 Map ID 在系统中都是独一无二的。
     *
     * 每 128*128 的分区会被分为一个块，按照从左到右、从上到下的顺序陈列在数组内。
     */
    abstract val tileMapIds: IntArray

    abstract val imageData: T

    abstract fun changeOwnerName(name: String)

    abstract fun rename(name: String)

    abstract fun replaceData(data: T)
}
