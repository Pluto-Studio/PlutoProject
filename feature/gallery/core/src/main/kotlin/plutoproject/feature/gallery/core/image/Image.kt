package plutoproject.feature.gallery.core.image

import java.util.*

private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,16}$")

class Image(
    val id: UUID,
    val type: ImageType,
    val owner: UUID,
    ownerName: String,
    name: String,
    val widthBlocks: Int,
    val heightBlocks: Int,
    val tileMapIds: IntArray,
) {
    var ownerName = ownerName
        private set

    var name = name
        private set

    internal fun changeOwnerName(name: String) {
        require(USERNAME_REGEX.matches(name)) { "Owner username $name must be a valid Minecraft username" }
        ownerName = name
    }

    internal fun rename(name: String) {
        this.name = name
    }
}
