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
     * 地图画在 X 轴方向的方块数。
     *
     * 实际 X 分辨率为 [mapXBlocks] * 128。
     */
    abstract val mapXBlocks: Int

    /**
     * 地图画在 Y 轴方向的方块数。
     *
     * 实际 Y 分辨率为 [mapYBlocks] * 128。
     */
    abstract val mapYBlocks: Int

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
