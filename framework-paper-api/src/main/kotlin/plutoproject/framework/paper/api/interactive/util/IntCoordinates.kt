package plutoproject.framework.paper.api.interactive.util

@JvmInline
value class IntCoordinates(val pair: Long) {
    val x get() = (pair shr 32).toInt()
    val y get() = pair.toInt()

    constructor(x: Int, y: Int) : this((x.toLong() shl 32) or y.toLong())

    operator fun component1() = x

    operator fun component2() = y

    operator fun plus(other: IntCoordinates) = IntCoordinates(x + other.x, y + other.y)

    override fun toString(): String = "($x, $y)"
}

typealias IntOffset = IntCoordinates

@JvmInline
value class IntSize(val pair: Long) {
    val width get() = (pair shr 32).toInt()
    val height get() = pair.toInt()

    constructor(width: Int, height: Int) : this((width.toLong() shl 32) or height.toLong())

    operator fun component1() = width

    operator fun component2() = height

    override fun toString(): String = "($width, $height)"
}
