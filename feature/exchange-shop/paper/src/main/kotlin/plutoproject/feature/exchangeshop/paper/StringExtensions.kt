package plutoproject.feature.exchangeshop.paper

fun String.isValidIdentifier(): Boolean {
    if (isEmpty() || isBlank()) return false
    return this.matches(Regex("^[A-Za-z0-9_]+$"))
}
