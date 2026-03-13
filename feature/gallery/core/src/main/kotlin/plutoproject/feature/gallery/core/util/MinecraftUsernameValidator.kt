package plutoproject.feature.gallery.core.util

private val MINECRAFT_USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,16}$")

internal fun isValidOwnerName(name: String): Boolean {
    return MINECRAFT_USERNAME_REGEX.matches(name)
}
