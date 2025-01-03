package plutoproject.framework.paper.util.chat

import java.awt.Color
import org.bukkit.Color as BukkitColor

fun Color.toBukkitColor(): BukkitColor = BukkitColor.fromARGB(alpha, red, green, blue)
