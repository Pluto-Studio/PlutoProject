package plutoproject.framework.common.util.time

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val Int.ticks: Duration get() = (this * 50).toDuration(DurationUnit.MILLISECONDS)

val Long.ticks: Duration get() = (this * 50).toDuration(DurationUnit.MILLISECONDS)

fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
