package plutoproject.foundation.common.text

import java.text.DecimalFormat

fun Double.trimmedString(): String = toBigDecimal().stripTrailingZeros().toPlainString()

fun Double.toCurrencyFormattedString(): String = DecimalFormat("#,##0.00").format(this)
