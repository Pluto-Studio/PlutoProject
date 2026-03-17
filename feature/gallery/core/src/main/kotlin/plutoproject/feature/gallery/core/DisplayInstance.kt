package plutoproject.feature.gallery.core

import java.util.*

class DisplayInstance(
    val id: UUID,
    val belongsTo: UUID,
    val world: String,
    val chunkX: Int,
    val chunkZ: Int,
    val facing: ItemFrameFacing,
    val widthBlocks: Int,
    val heightBlocks: Int,

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的 X 坐标。
     */
    val originX: Double,

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的 Y 坐标。
     */
    val originY: Double,

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的 Z 坐标。
     */
    val originZ: Double,

    /**
     * 索引为 Tile Index，用于通过索引 ID 取对应的物品展示框 UUID，或者反过来。
     */
    val itemFrameIds: List<UUID>
)


enum class ItemFrameFacing {
    NORTH, SOUTH, EAST, WEST, UP, DOWN
}
