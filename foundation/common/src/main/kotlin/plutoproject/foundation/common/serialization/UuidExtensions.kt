package plutoproject.foundation.common.serialization

import java.util.UUID

fun String.uuid(): UUID = UUID.fromString(this)

fun String.uuidOrNull(): UUID? = runCatching(UUID::fromString).getOrNull()
