package plutoproject.framework.common.util.time

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

fun ZoneId.toOffset(): ZoneOffset {
    return rules.getOffset(Instant.now())
}
