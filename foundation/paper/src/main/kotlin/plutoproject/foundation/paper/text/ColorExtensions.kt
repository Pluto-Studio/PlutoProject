package plutoproject.foundation.paper.text

import java.awt.Color
import org.bukkit.Color as BukkitColor

fun Color.toBukkitColor(): BukkitColor = BukkitColor.fromARGB(alpha, red, green, blue)
