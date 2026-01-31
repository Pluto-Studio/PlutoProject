package plutoproject.framework.common.util.time

import java.time.*
import java.time.format.DateTimeFormatter

private val morningDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd 上午 hh:mm:ss")
private val afternoonDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd 下午 hh:mm:ss")
private val dateTime24HourFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
private val morningTimeFormatter = DateTimeFormatter.ofPattern("上午 hh:mm:ss")
private val afternoonTimeFormatter = DateTimeFormatter.ofPattern("下午 hh:mm:ss")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

fun LocalDateTime.format(): String =
    if (hour < 12) morningDateFormatter.format(this) else afternoonDateFormatter.format(this)

fun LocalDateTime.format24Hour(): String = dateTime24HourFormatter.format(this)

fun LocalDateTime.formatTime(): String =
    if (hour < 12) morningTimeFormatter.format(this) else afternoonTimeFormatter.format(this)

fun LocalDateTime.formatDate(): String = dateFormatter.format(this)

fun ZonedDateTime.format(): String =
    if (hour < 12) morningDateFormatter.format(this) else afternoonDateFormatter.format(this)

fun ZonedDateTime.format24Hour(): String = dateTime24HourFormatter.format(this)

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

fun Instant.format(zoneId: ZoneId = LocalZoneId): String =
    atZone(zoneId).format()

fun Instant.format24Hour(zoneId: ZoneId = LocalZoneId): String =
    atZone(zoneId).format24Hour()

fun Instant.formatTime(zoneId: ZoneId = LocalZoneId): String =
    atZone(zoneId).formatTime()

fun Instant.formatDate(zoneId: ZoneId = LocalZoneId): String =
    atZone(zoneId).formatDate()
