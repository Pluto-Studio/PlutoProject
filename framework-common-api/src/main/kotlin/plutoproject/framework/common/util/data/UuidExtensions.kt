package plutoproject.framework.common.util.data

import java.util.*

fun UUID.toShortUuidString(): String = toString().replace("-", "")
