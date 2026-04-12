package plutoproject.feature.gallery.core.util

@JvmInline
value class ChunkKey(val value: Long) {
    constructor(x: Int, z: Int) : this((x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL))

    val x: Int
        get() = (value ushr 32).toInt()

    val z: Int
        get() = value.toInt()
}
