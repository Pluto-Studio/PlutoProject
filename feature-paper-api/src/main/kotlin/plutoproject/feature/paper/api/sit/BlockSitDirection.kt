package plutoproject.feature.paper.api.sit

sealed class BlockSitDirection(val yaw: Float) {
    data object South : BlockSitDirection(0.0f)
    data object West : BlockSitDirection(90.0f)
    data object North : BlockSitDirection(180.0f)
    data object East : BlockSitDirection(270.0f)

    data object SouthWest : BlockSitDirection(45.0f)
    data object NorthWest : BlockSitDirection(135.0f)
    data object NorthEast : BlockSitDirection(225.0f)
    data object SouthEast : BlockSitDirection(315.0f)

    class Custom(yaw: Float) : BlockSitDirection(yaw)

    companion object {
        fun fromYaw(yaw: Float): BlockSitDirection {
            val normalized = (yaw % 360 + 360) % 360

            return when (normalized) {
                in 0.0..22.5, in 337.5..360.0 -> South
                in 22.5..67.5 -> SouthWest
                in 67.5..112.5 -> West
                in 112.5..157.5 -> NorthWest
                in 157.5..202.5 -> North
                in 202.5..247.5 -> NorthEast
                in 247.5..292.5 -> East
                in 292.5..337.5 -> SouthEast
                else -> Custom(yaw)
            }
        }
    }
}
