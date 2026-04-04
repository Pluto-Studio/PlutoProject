package plutoproject.feature.gallery.api

import java.util.UUID

/**
 * 代表一幅地图画在世界中的一次展出实例。
 */
interface ImageDisplay {
    /**
     * 该展出实例的唯一 ID。
     */
    val id: UUID

    /**
     * 本展出所对应的地图画 ID。
     */
    val imageId: UUID

    /**
     * 本展出所在的世界名称。
     */
    val world: String

    /**
     * 本展出所在区块的 X 坐标。
     */
    val chunkX: Int

    /**
     * 本展出所在区块的 Z 坐标。
     */
    val chunkZ: Int

    /**
     * 本展出的朝向。
     */
    val facing: ItemFrameFacing

    /**
     * 本展出占据的宽度，单位为方块。
     */
    val widthBlocks: Int

    /**
     * 本展出占据的高度，单位为方块。
     */
    val heightBlocks: Int

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心 X 坐标。
     */
    val originX: Double

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心 Y 坐标。
     */
    val originY: Double

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心 Z 坐标。
     */
    val originZ: Double

    /**
     * 索引为 Tile Index，用于通过索引 ID 取对应的物品展示框 UUID，或者反过来。
     */
    val itemFrameIds: List<UUID>
}
