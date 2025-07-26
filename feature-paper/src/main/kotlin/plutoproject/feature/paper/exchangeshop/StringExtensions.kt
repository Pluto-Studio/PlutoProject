package plutoproject.feature.paper.exchangeshop

fun String.isAlphabeticOrUnderscore(): Boolean {
    return this.matches(Regex("^[A-Za-z_]+$"))
}
