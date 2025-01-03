package plutoproject.framework.common.util.time

import java.time.ZoneId
import java.util.*

val LocalZoneId: ZoneId = ZoneId.systemDefault()

val LocalTimeZone: TimeZone = TimeZone.getTimeZone(LocalZoneId)
