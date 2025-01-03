package plutoproject.framework.common.util.chat

import net.kyori.adventure.text.format.TextColor
import java.awt.Color

fun Color.toTextColor(): TextColor {
    return TextColor.color(red, green, blue)
}
