package plutoproject.feature.gallery.core.util

internal fun clampToByte(value: Int): Int = value.coerceIn(0, 255)
