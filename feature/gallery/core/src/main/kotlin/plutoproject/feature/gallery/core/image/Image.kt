package plutoproject.feature.gallery.core.image

import java.util.*

private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,16}$")

class Image(
    val id: UUID,
    val type: ImageType,
    val owner: UUID,
    val ownerName: String,
    val name: String,
    val widthBlocks: Int,
    val heightBlocks: Int,
    val tileMapIds: IntArray,
) {
    fun withOwnerName(name: String): Image {
        require(USERNAME_REGEX.matches(name)) { "Owner username $name must be a valid Minecraft username" }
        return copy(
            ownerName = name,
        )
    }

    fun renamed(name: String): Image {
        return copy(
            name = name,
        )
    }

    private fun copy(
        ownerName: String = this.ownerName,
        name: String = this.name,
    ): Image {
        return Image(
            id = id,
            type = type,
            owner = owner,
            ownerName = ownerName,
            name = name,
            widthBlocks = widthBlocks,
            heightBlocks = heightBlocks,
            tileMapIds = tileMapIds,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Image) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
