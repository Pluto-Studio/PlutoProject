package plutoproject.feature.paper.sit.strategies

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace.*
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.type.Stairs
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.sit.BlockSitDirection
import plutoproject.feature.paper.api.sit.BlockSitStrategy

object StairBlockSitStrategy : BlockSitStrategy {
    override fun match(block: Block): Boolean {
        return block.blockData is Stairs
    }

    override fun isAllowed(block: Block): Boolean {
        return true
    }

    override fun shouldSitOnRightClick(player: Player, block: Block): Boolean {
        val stairs = block.blockData as Stairs
        return stairs.half == Bisected.Half.BOTTOM
    }

    private val Stairs.Shape.isLeft: Boolean
        get() = this == Stairs.Shape.INNER_LEFT || this == Stairs.Shape.OUTER_LEFT

    private val Stairs.Shape.isRight: Boolean
        get() = this == Stairs.Shape.INNER_RIGHT || this == Stairs.Shape.OUTER_RIGHT

    private val Stairs.Shape.isStraight: Boolean
        get() = this == Stairs.Shape.STRAIGHT

    override fun getSitLocation(player: Player, block: Block): Location {
        val stairs = block.blockData as Stairs
        val facing = stairs.facing
        val shape = stairs.shape

        val defaultLocation = DefaultBlockSitStrategy.getSitLocation(player, block)
        var offsetX = 0.0
        val offsetY = -0.5
        var offsetZ = 0.0

        when (facing) {
            EAST -> {
                offsetX = -0.25
                if (shape.isLeft) offsetZ += 0.25 else if (shape.isRight) offsetZ += -0.25
            }

            SOUTH -> {
                offsetZ = -0.25
                if (shape.isLeft) offsetX += -0.25 else if (shape.isRight) offsetZ += 0.25
            }

            WEST -> {
                offsetX = 0.25
                if (shape.isLeft) offsetZ += -0.25 else if (shape.isRight) offsetZ += 0.25
            }

            NORTH -> {
                offsetZ += 0.25
                if (shape.isLeft) offsetX += 0.25 else if (shape.isRight) offsetX += -0.25
            }


            else -> error("Unreachable")
        }

        return defaultLocation.clone().add(offsetX, offsetY, offsetZ)
    }

    override fun getSitDirection(player: Player, block: Block): BlockSitDirection {
        val stairs = block.blockData as Stairs
        val facing = stairs.facing
        val shape = stairs.shape

        return when (facing) {
            EAST -> {
                when {
                    shape.isLeft -> BlockSitDirection.SouthWest
                    shape.isRight -> BlockSitDirection.NorthWest
                    shape.isStraight -> BlockSitDirection.West
                    else -> error("Unreachable")
                }
            }

            SOUTH -> {
                when {
                    shape.isLeft -> BlockSitDirection.NorthWest
                    shape.isRight -> BlockSitDirection.NorthEast
                    shape.isStraight -> BlockSitDirection.North
                    else -> error("Unreachable")
                }
            }

            WEST -> {
                when {
                    shape.isLeft -> BlockSitDirection.NorthEast
                    shape.isRight -> BlockSitDirection.SouthEast
                    shape.isStraight -> BlockSitDirection.East
                    else -> error("Unreachable")
                }
            }

            NORTH -> {
                when {
                    shape.isLeft -> BlockSitDirection.SouthEast
                    shape.isRight -> BlockSitDirection.SouthWest
                    shape.isStraight -> BlockSitDirection.South
                    else -> error("Unreachable")
                }
            }


            else -> error("Unreachable")
        }
    }
}
