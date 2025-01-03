package plutoproject.framework.common.util.data

import java.util.*

fun String.convertShortUuidToLong(): String {
    check(length == 32) { "Not a valid short uuid" }
    return replace(
        Regex("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})"),
        "$1-$2-$3-$4-$5"
    )
}

fun String.convertToUuid(): UUID = UUID.fromString(this)

fun String.convertToUuidOrNull(): UUID? =
    try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        null
    }
