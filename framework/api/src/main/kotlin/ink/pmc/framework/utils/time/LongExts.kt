package ink.pmc.framework.utils.time

import java.time.Instant

inline val Long.instant: Instant
    get() = Instant.ofEpochMilli(this)