package plutoproject.foundation.common.text

fun Double.trimmedString(): String = toBigDecimal().stripTrailingZeros().toPlainString()
