package plutoproject.feature.gallery.core

import java.util.*

data class DisplayInstance(
    val id: UUID,
    val belongsTo: UUID,
    val world: String,
    val chunkX: Int,
    val chunkZ: Int,
    val facing: ItemFrameFacing,
    val widthBlocks: Int,
    val heightBlocks: Int,

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心 X 坐标。
     */
    val originX: Double,

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心 Y 坐标。
     */
    val originY: Double,

    /**
     * 第一个展示框（Tile ID 为 0，地图画左上角）的中心 Z 坐标。
     */
    val originZ: Double,

    /**
     * 索引为 Tile Index，用于通过索引 ID 取对应的物品展示框 UUID，或者反过来。
     */
    val itemFrameIds: List<UUID>
) {
    fun buildGeometry(): DisplayGeometry {
        val origin = Vec3(originX, originY, originZ)

        val (axisU, axisV, normal) = when (facing) {
            ItemFrameFacing.NORTH -> Triple(
                Vec3(-1.0, 0.0, 0.0),
                Vec3(0.0, -1.0, 0.0),
                Vec3(0.0, 0.0, -1.0)
            )

            ItemFrameFacing.SOUTH -> Triple(
                Vec3(1.0, 0.0, 0.0),
                Vec3(0.0, -1.0, 0.0),
                Vec3(0.0, 0.0, 1.0)
            )

            ItemFrameFacing.EAST -> Triple(
                Vec3(0.0, 0.0, -1.0),
                Vec3(0.0, -1.0, 0.0),
                Vec3(1.0, 0.0, 0.0)
            )

            ItemFrameFacing.WEST -> Triple(
                Vec3(0.0, 0.0, 1.0),
                Vec3(0.0, -1.0, 0.0),
                Vec3(-1.0, 0.0, 0.0)
            )

            ItemFrameFacing.UP -> Triple(
                Vec3(1.0, 0.0, 0.0),
                Vec3(0.0, 0.0, 1.0),
                Vec3(0.0, 1.0, 0.0)
            )

            ItemFrameFacing.DOWN -> Triple(
                Vec3(1.0, 0.0, 0.0),
                Vec3(0.0, 0.0, -1.0),
                Vec3(0.0, -1.0, 0.0)
            )
        }

        val center =
            origin + axisU * ((widthBlocks - 1) / 2.0) + axisV * ((heightBlocks - 1) / 2.0)

        return DisplayGeometry(
            origin = origin,
            center = center,
            axisU = axisU,
            axisV = axisV,
            normal = normal,
            widthBlocks = widthBlocks,
            heightBlocks = heightBlocks,
        )
    }
}

enum class ItemFrameFacing {
    NORTH, SOUTH, EAST, WEST, UP, DOWN
}
