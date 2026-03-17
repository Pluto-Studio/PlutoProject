package plutoproject.feature.gallery.core

import kotlin.math.sqrt

data class Vec3(
    val x: Double,
    val y: Double,
    val z: Double
) {
    operator fun plus(other: Vec3): Vec3 =
        Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 =
        Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scale: Double): Vec3 =
        Vec3(x * scale, y * scale, z * scale)

    fun dot(other: Vec3): Double =
        x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 =
        Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )

    fun lengthSquared(): Double =
        x * x + y * y + z * z

    fun normalizedOrNull(): Vec3? {
        val len2 = lengthSquared()
        if (len2 < 1e-12) return null
        val invLen = 1.0 / kotlin.math.sqrt(len2)
        return Vec3(x * invLen, y * invLen, z * invLen)
    }
}
