package plutoproject.framework.common.util.chat

import java.awt.Color
import java.text.DecimalFormat

fun Int.toRgbaColor(): Color = Color(
    (this shr 16) and 0xFF,
    (this shr 8) and 0xFF,
    this and 0xFF,
    (this shr 24) and 0xFF
)

fun Double.toCurrencyFormattedString(): String {
    val decimalFormat = DecimalFormat("#,##0.00")
    return decimalFormat.format(this)
}
