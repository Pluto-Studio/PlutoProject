package plutoproject.framework.common.util.data

import java.util.*

fun String.convertShortUuidToLong(): String {
    check(length == 32) { "Not a valid short uuid" }
    return replace(
        Regex("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})"),
        "$1-$2-$3-$4-$5"
    )
}

/**
 * 将字符串内容转换为 UUID。
 *
 * @return 转换后的 UUID。
 * @throws IllegalArgumentException 若字符串不是合法的 UUID
 */

fun String.uuid(): UUID = UUID.fromString(this)

/**
 * 将字符串内容转换为 UUID。
 *
 * @return 转换后的 UUID，若字符串内容不是合法的 UUID 则为 null
 */
fun String.uuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
