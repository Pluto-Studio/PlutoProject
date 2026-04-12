package plutoproject.feature.gallery.api

import java.util.UUID

/**
 * 代表一幅地图画。
 */
interface Image {
    /**
     * 该地图画的唯一 ID。
     */
    val id: UUID

    /**
     * 该地图画的类型。
     */
    val type: ImageType

    /**
     * 该地图画拥有者的玩家 UUID。
     */
    val owner: UUID

    /**
     * 该地图画拥有者的玩家名。
     */
    val ownerName: String

    /**
     * 该地图画的名称。
     */
    val name: String

    /**
     * 该地图画占据的宽度，单位为方块。
     */
    val widthBlocks: Int

    /**
     * 该地图画占据的高度，单位为方块。
     */
    val heightBlocks: Int

    /**
     * 该地图画每个 Tile 对应的地图 ID。
     */
    val tileMapIds: IntArray
}
