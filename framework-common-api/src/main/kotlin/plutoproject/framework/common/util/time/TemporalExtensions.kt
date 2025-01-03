package plutoproject.framework.common.util.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val morningDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd 上午 hh:mm:ss")
private val afternoonDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd 下午 hh:mm:ss")
private val morningTimeFormatter = DateTimeFormatter.ofPattern("上午 hh:mm:ss")
private val afternoonTimeFormatter = DateTimeFormatter.ofPattern("下午 hh:mm:ss")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

fun LocalDateTime.format(): String =
    if (hour < 12) morningDateFormatter.format(this) else afternoonDateFormatter.format(this)

fun LocalDateTime.formatTime(): String =
    if (hour < 12) morningTimeFormatter.format(this) else afternoonTimeFormatter.format(this)

fun LocalDateTime.formatDate(): String = dateFormatter.format(this)

fun ZonedDateTime.format(): String =
    if (hour < 12) morningDateFormatter.format(this) else afternoonDateFormatter.format(this)

fun ZonedDateTime.formatTime(): String =
    if (hour < 12) morningTimeFormatter.format(this) else afternoonTimeFormatter.format(this)

fun ZonedDateTime.formatDate(): String = dateFormatter.format(this)

fun LocalDate.formatDate(): String = dateFormatter.format(this)

fun LocalDate.atEndOfDay(): LocalDateTime {
    return atTime(23, 59, 59)
}

fun YearMonth.atStartOfMonth(): LocalDate {
    return atDay(1)
}
