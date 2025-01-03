package plutoproject.framework.common.util.chat

import net.kyori.adventure.text.Component
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

fun Duration.toFormattedString(): String {
    val totalNanoseconds = inWholeNanoseconds
    val days = totalNanoseconds / 86_400_000_000_000
    val hours = (totalNanoseconds % 86_400_000_000_000) / 3_600_000_000_000
    val minutes = (totalNanoseconds % 3_600_000_000_000) / 60_000_000_000
    val seconds = (totalNanoseconds % 60_000_000_000) / 1_000_000_000
    val milliseconds = (totalNanoseconds % 1_000_000_000) / 1_000_000
    val microseconds = (totalNanoseconds % 1_000_000) / 1_000
    val nanoseconds = totalNanoseconds % 1_000

    val result = StringBuilder()

    if (days > 0) result.append("$days 天 ")
    if (hours > 0) result.append("$hours 小时 ")

    if (minutes > 0) {
        if (result.isNotEmpty()) {
            result.append("$minutes 分 ")
        } else {
            result.append("$minutes 分钟 ")
        }
    }

    if (seconds > 0) result.append("$seconds 秒 ")
    if (milliseconds > 0) result.append("$milliseconds 毫秒 ")
    if (microseconds > 0) result.append("$microseconds 微秒 ")
    if (nanoseconds > 0) result.append("$nanoseconds 纳秒")

    if (result.isEmpty()) {
        result.append("0 毫秒 ")
    }

    // 检查最后一个单位是否是分钟
    if (result.endsWith(" 分 ")) {
        result.replace(result.length - 2, result.length, "分钟 ")
    }

    return result.toString().trim()
}

fun JavaDuration.toFormattedString() = toKotlinDuration().toFormattedString()

fun Duration.toFormattedComponent(): Component = Component.text(toFormattedString())

fun JavaDuration.toFormattedComponent(): Component = Component.text(toFormattedString())
