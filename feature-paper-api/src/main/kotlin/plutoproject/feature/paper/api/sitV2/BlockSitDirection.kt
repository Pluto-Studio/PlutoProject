package plutoproject.feature.paper.api.sitV2

sealed class BlockSitDirection(val yaw: Float) {
    data object South : BlockSitDirection(0.0f)
    data object SouthWest : BlockSitDirection(-45.0f)
    data object West : BlockSitDirection(-90.0f)
    data object NorthWest : BlockSitDirection(-135.0f)
    data object North : BlockSitDirection(180.0f)
    data object NorthEast : BlockSitDirection(135.0f)
    data object East : BlockSitDirection(90.0f)
    data object SouthEast : BlockSitDirection(45.0f)

    class Custom(yaw: Float) : BlockSitDirection(yaw)

    companion object {
        fun fromYaw(yaw: Float): BlockSitDirection {
            val normalized = ((yaw % 360 + 360) % 360) // yaw in [0, 360)
            return when (normalized) {
                in 337.5..360.0, in 0.0..22.5 -> South
                in 22.5..67.5 -> SouthEast
                in 67.5..112.5 -> East
                in 112.5..157.5 -> NorthEast
                in 157.5..202.5 -> North
                in 202.5..247.5 -> NorthWest
                in 247.5..292.5 -> West
                in 292.5..337.5 -> SouthWest
                else -> Custom(yaw)
            }
        }
    }
}
