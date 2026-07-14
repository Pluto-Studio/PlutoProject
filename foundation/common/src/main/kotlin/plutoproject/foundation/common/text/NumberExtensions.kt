package plutoproject.foundation.common.text

import java.awt.Color
import java.text.DecimalFormat

fun Int.toRgbaColor(): Color = Color(
    (this shr 16) and 0xFF,
    (this shr 8) and 0xFF,
    this and 0xFF,
    (this shr 24) and 0xFF,
)

fun Double.trimmedString(): String = toBigDecimal().stripTrailingZeros().toPlainString()

fun Double.toCurrencyFormattedString(): String = DecimalFormat("#,##0.00").format(this)
