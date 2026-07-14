package plutoproject.foundation.paper.inventory

import org.bukkit.Material

val Material.isOpenableBook: Boolean
    get() = this == Material.WRITTEN_BOOK || this == Material.WRITABLE_BOOK
