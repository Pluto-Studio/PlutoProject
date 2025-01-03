package plutoproject.framework.common.util

import kotlin.math.roundToInt

fun Double.toTrimmedString(): String {
    var result = toBigDecimal().stripTrailingZeros().toPlainString()
    if (result.endsWith(".")) {
        result = result.dropLast(1)
    }
    return result
}

fun Double.roundTo2(): Double {
    return ((this * 100).roundToInt() / 100.0)
}
